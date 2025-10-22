@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.utils.setDisallowChanges
import io.sentry.android.gradle.SentryPlugin.Companion.sep
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.SentryTasksProvider.getAssembleTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getMappingFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.services.SentryModulesService
import io.sentry.android.gradle.sourcecontext.OutputPaths
import io.sentry.android.gradle.sourcecontext.SourceContext
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask
import io.sentry.android.gradle.tasks.InjectSentryMetaPropertiesIntoAssetsTask
import io.sentry.android.gradle.tasks.PropertiesFileOutputTask
import io.sentry.android.gradle.tasks.SentryGenerateIntegrationListTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadAppArtifactTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.tasks.configureNativeSymbolsTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskV2
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.util.GroovyCompat
import io.sentry.android.gradle.util.ReleaseInfo
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.SentryPluginUtils.isMinificationEnabled
import io.sentry.android.gradle.util.SentryPluginUtils.isVariantAllowed
import io.sentry.android.gradle.util.collectModules
import io.sentry.android.gradle.util.hookWithAssembleTasks
import io.sentry.android.gradle.util.hookWithMinifyTasks
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal

fun ApplicationAndroidComponentsExtension.configure(
  project: Project,
  extension: SentryPluginExtension,
  buildEvents: BuildEventListenerRegistryInternal,
  cliExecutable: Provider<String>,
  sentryOrg: String?,
  sentryProject: String?,
) {
  // temp folder for sentry-related stuff
  val tmpDir = File("${project.buildDir}${sep}tmp${sep}sentry")
  tmpDir.mkdirs()

  onVariants { variant ->
    if (isVariantAllowed(extension, variant.name, variant.flavorName, variant.buildType)) {
      val paths = OutputPaths(project, variant.name)
      val sentryTelemetryProvider =
        variant.configureTelemetry(project, extension, cliExecutable, sentryOrg, buildEvents)

      variant.configureDependenciesTask(project, extension, sentryTelemetryProvider)

      // TODO: do this only once, and all other tasks should be SentryVariant.configureSomething
      val sentryVariant = AndroidVariant74(variant)

      val additionalSourcesProvider =
        project.provider {
          extension.additionalSourceDirsForSourceContext.getOrElse(emptySet()).map {
            project.layout.projectDirectory.dir(it)
          }
        }
      val sourceFiles = sentryVariant.sources(project, additionalSourcesProvider)

      val tasksGeneratingProperties = mutableListOf<TaskProvider<out PropertiesFileOutputTask>>()
      val sourceContextTasks =
        variant.configureSourceBundleTasks(
          project,
          extension,
          sentryTelemetryProvider,
          paths,
          sourceFiles,
          cliExecutable,
          sentryOrg,
          sentryProject,
        )
      sourceContextTasks?.let { tasksGeneratingProperties.add(it.generateBundleIdTask) }

      val generateProguardUuidTask =
        variant.configureProguardMappingsTasks(
          project,
          extension,
          sentryTelemetryProvider,
          paths,
          cliExecutable,
          sentryOrg,
          sentryProject,
        )
      generateProguardUuidTask?.let { tasksGeneratingProperties.add(it) }

      val generateDistributionPropertiesTask =
        variant.configureDistributionPropertiesTask(
          project,
          extension,
          sentryTelemetryProvider,
          paths,
          sentryOrg,
          sentryProject,
        )
      generateDistributionPropertiesTask?.let { tasksGeneratingProperties.add(it) }

      sentryVariant.configureNativeSymbolsTask(
        project,
        extension,
        sentryTelemetryProvider,
        cliExecutable,
        sentryOrg,
        sentryProject,
      )

      // we can't hook into asset generation, nor manifest merging, as all those tasks
      // are dependencies of the compilation / minification task
      // and as our ProGuard UUID depends on minification itself; creating a
      // circular dependency
      // instead, we transform all assets and inject the properties file
      sentryVariant.apply {
        val injectAssetsTask =
          InjectSentryMetaPropertiesIntoAssetsTask.register(
            project,
            extension,
            sentryTelemetryProvider,
            tasksGeneratingProperties,
            variant.name.capitalized,
          )

        assetsWiredWithDirectories(
          injectAssetsTask,
          InjectSentryMetaPropertiesIntoAssetsTask::inputDir,
          InjectSentryMetaPropertiesIntoAssetsTask::outputDir,
        )

        // flutter doesn't use the transform API
        // and manually wires up task dependencies,
        // which causes errors like this:
        //      Task ':app:injectSentryDebugMetaPropertiesIntoAssetsDebug' uses this output of task
        // ':app:copyFlutterAssetsDebug' without declaring an explicit or implicit dependency
        // thus we have to manually add the task dependency
        project.afterEvaluate {
          // https://github.com/flutter/flutter/blob/6ce591f7ea3ba827d9340ce03f7d8e3a37ebb03a/packages/flutter_tools/gradle/src/main/groovy/flutter.groovy#L1295-L1298
          project.tasks.findByName("copyFlutterAssets${variant.name.capitalized}")?.let {
            flutterAssetsTask ->
            injectAssetsTask.configure { injectTask -> injectTask.dependsOn(flutterAssetsTask) }
          }
        }
      }

      if (extension.tracingInstrumentation.enabled.get()) {
        /**
         * We detect sentry-android SDK version using configurations.incoming.afterResolve. This is
         * guaranteed to be executed BEFORE any of the build tasks/transforms are started.
         *
         * After detecting the sdk state, we use Gradle's shared build service to persist the state
         * between builds and also during a single build, because transforms are run in parallel.
         */
        val sentryModulesService =
          SentryModulesService.register(
            project,
            extension.tracingInstrumentation.features,
            extension.tracingInstrumentation.logcat.enabled,
            extension.includeSourceContext,
            extension.dexguardEnabled,
            extension.tracingInstrumentation.appStart.enabled,
          )
        /**
         * We have to register SentryModulesService as a build event listener, so it will not be
         * discarded after the configuration phase (where we store the collected dependencies), and
         * will be passed down to the InstrumentationFactory
         */
        buildEvents.onTaskCompletion(sentryModulesService)

        project.collectModules(
          "${variant.name}RuntimeClasspath",
          variant.name,
          sentryModulesService,
        )

        variant.configureInstrumentation(
          SpanAddingClassVisitorFactory::class.java,
          InstrumentationScope.ALL,
          FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
          extension.tracingInstrumentation.excludes,
        ) { params ->
          if (extension.tracingInstrumentation.forceInstrumentDependencies.get()) {
            params.invalidate.setDisallowChanges(System.currentTimeMillis())
          }
          params.debug.setDisallowChanges(extension.tracingInstrumentation.debug.get())
          params.logcatMinLevel.setDisallowChanges(extension.tracingInstrumentation.logcat.minLevel)

          params.sentryModulesService.setDisallowChanges(sentryModulesService)
          params.features.setDisallowChanges(extension.tracingInstrumentation.features)
          params.logcatEnabled.setDisallowChanges(extension.tracingInstrumentation.logcat.enabled)
          params.appStartEnabled.setDisallowChanges(
            extension.tracingInstrumentation.appStart.enabled
          )
          params.tmpDir.set(tmpDir)
        }

        val manifestUpdater =
          SentryGenerateIntegrationListTask.register(
            project,
            extension,
            sentryTelemetryProvider,
            sentryModulesService,
            variant.name,
          )

        variant.artifacts
          .use(manifestUpdater)
          .wiredWithFiles(
            SentryGenerateIntegrationListTask::mergedManifest,
            SentryGenerateIntegrationListTask::updatedManifest,
          )
          .toTransform(SingleArtifact.MERGED_MANIFEST)
      }
      val sizeAnalysisEnabled = extension.sizeAnalysis.enabled.get() == true
      val distributionEnabled = extension.distribution.enabledVariants.get().contains(variant.name)
      if (sizeAnalysisEnabled || distributionEnabled) {
        variant.configureUploadAppTasks(
          project,
          extension,
          sentryTelemetryProvider,
          cliExecutable,
          sentryOrg,
          sentryProject,
        )
      }
    }
  }
}

private fun Variant.configureTelemetry(
  project: Project,
  extension: SentryPluginExtension,
  cliExecutable: Provider<String>,
  sentryOrg: String?,
  buildEvents: BuildEventListenerRegistryInternal,
): Provider<SentryTelemetryService> {
  val variant = AndroidVariant74(this)
  val sentryTelemetryProvider = SentryTelemetryService.register(project)
  project.gradle.taskGraph.whenReady {
    sentryTelemetryProvider.get().start {
      SentryTelemetryService.createParameters(
        project,
        variant,
        extension,
        cliExecutable,
        sentryOrg,
        "Android",
      )
    }
    buildEvents.onOperationCompletion(sentryTelemetryProvider)
  }
  return sentryTelemetryProvider
}

private fun Variant.configureSourceBundleTasks(
  project: Project,
  extension: SentryPluginExtension,
  sentryTelemetryProvider: Provider<SentryTelemetryService>,
  paths: OutputPaths,
  sourceFiles: Provider<out Collection<Directory>>?,
  cliExecutable: Provider<String>,
  sentryOrg: String?,
  sentryProject: String?,
): SourceContext.SourceContextTasks? {
  if (extension.includeSourceContext.get()) {
    val taskSuffix = name.capitalized
    val variant = AndroidVariant74(this)

    val sourceContextTasks =
      SourceContext.register(
        project,
        extension,
        sentryTelemetryProvider,
        variant,
        paths,
        sourceFiles,
        cliExecutable,
        sentryOrg,
        sentryProject,
        taskSuffix,
      )

    if (variant.buildTypeName == "release") {
      sourceContextTasks.uploadSourceBundleTask.hookWithAssembleTasks(project, variant)
    }

    return sourceContextTasks
  } else {
    return null
  }
}

private fun Variant.configureDependenciesTask(
  project: Project,
  extension: SentryPluginExtension,
  sentryTelemetryProvider: Provider<SentryTelemetryService>,
) {
  if (extension.includeDependenciesReport.get()) {
    val reportDependenciesTask =
      SentryExternalDependenciesReportTaskV2.register(
        project = project,
        extension,
        sentryTelemetryProvider,
        configurationName = "${name}RuntimeClasspath",
        attributeValueJar = "android-classes",
        includeReport = extension.includeDependenciesReport,
        taskSuffix = name.capitalized,
      )
    sources.assets?.addGeneratedSourceDirectory(reportDependenciesTask) { task -> task.output }
  }
}

private fun ApplicationVariant.configureProguardMappingsTasks(
  project: Project,
  extension: SentryPluginExtension,
  sentryTelemetryProvider: Provider<SentryTelemetryService>,
  paths: OutputPaths,
  cliExecutable: Provider<String>,
  sentryOrg: String?,
  sentryProject: String?,
): TaskProvider<SentryGenerateProguardUuidTask>? {
  val variant = AndroidVariant74(this)
  val sentryProps = getPropertiesFilePath(project, variant)
  val dexguardEnabled = extension.dexguardEnabled.get()
  val isMinifyEnabled = isMinificationEnabled(project, variant, dexguardEnabled)

  if (isMinifyEnabled && extension.includeProguardMapping.get()) {
    val generateUuidTask =
      SentryGenerateProguardUuidTask.register(
        project = project,
        extension,
        sentryTelemetryProvider,
        proguardMappingFile = getMappingFileProvider(project, variant, dexguardEnabled),
        taskSuffix = name.capitalized,
        output = paths.proguardUuidDir,
      )

    val releaseInfo = getReleaseInfo()
    val uploadMappingsTask =
      SentryUploadProguardMappingsTask.register(
        project = project,
        extension,
        sentryTelemetryProvider,
        debug = extension.debug,
        cliExecutable = cliExecutable,
        generateUuidTask = generateUuidTask,
        sentryProperties = sentryProps,
        mappingFiles = getMappingFileProvider(project, variant, dexguardEnabled),
        autoUploadProguardMapping = extension.autoUploadProguardMapping,
        sentryOrg = sentryOrg?.let { project.provider { it } } ?: extension.org,
        sentryProject = sentryProject?.let { project.provider { it } } ?: extension.projectName,
        sentryAuthToken = extension.authToken,
        sentryUrl = extension.url,
        taskSuffix = name.capitalized,
        releaseInfo = releaseInfo,
      )

    generateUuidTask.hookWithMinifyTasks(
      project,
      name,
      dexguardEnabled && GroovyCompat.isDexguardEnabledForVariant(project, name),
    )

    uploadMappingsTask.hookWithAssembleTasks(project, variant)

    return generateUuidTask
  } else {
    return null
  }
}

private fun ApplicationVariant.configureDistributionPropertiesTask(
  project: Project,
  extension: SentryPluginExtension,
  sentryTelemetryProvider: Provider<SentryTelemetryService>,
  paths: OutputPaths,
  sentryOrg: String?,
  sentryProject: String?,
): TaskProvider<GenerateDistributionPropertiesTask>? {
  val variantName = name
  if (extension.distribution.enabledVariants.get().contains(variantName)) {
    val variant = AndroidVariant74(this)
    // Distribution uses a custom auto-install implementation instead of the standard
    // InstallStrategy approach (see AutoInstall.kt) because it requires variant-specific
    // installation based on extension.distribution.enabledVariants, whereas other integrations
    // are installed globally when their dependencies are detected.
    if (extension.autoInstallation.enabled.get()) {
      val sentryVersion = extension.autoInstallation.sentryVersion.get()
      val configurationName = "${variantName}Implementation"
      project.dependencies.add(
        configurationName,
        "${io.sentry.android.gradle.autoinstall.SENTRY_GROUP}:${SentryModules.SENTRY_ANDROID_DISTRIBUTION.name}:$sentryVersion",
      )
      project.logger.info(
        "${SentryModules.SENTRY_ANDROID_DISTRIBUTION.name} was successfully installed for variant $variantName with version: $sentryVersion"
      )
    }

    return GenerateDistributionPropertiesTask.register(
      project = project,
      extension = extension,
      sentryTelemetryProvider = sentryTelemetryProvider,
      output = paths.distributionPropertiesDir,
      taskSuffix = name.capitalized,
      buildConfiguration = name,
      sentryOrg = sentryOrg,
      sentryProject = sentryProject,
      variant = variant,
    )
  }
  return null
}

/**
 * Configure the upload AAB and APK tasks and set them up as finalizers on the respective producer
 * tasks
 */
fun Variant.configureUploadAppTasks(
  project: Project,
  extension: SentryPluginExtension,
  sentryTelemetryProvider: Provider<SentryTelemetryService>,
  cliExecutable: Provider<String>,
  sentryOrg: String?,
  sentryProject: String?,
): Pair<TaskProvider<SentryUploadAppArtifactTask>, TaskProvider<SentryUploadAppArtifactTask>> {
  val variant = AndroidVariant74(this)
  val sentryProps = getPropertiesFilePath(project, variant)
  val buildVariant = variant.name
  val (uploadBundleTask, uploadApkTask) =
    SentryUploadAppArtifactTask.register(
      project = project,
      extension,
      sentryTelemetryProvider,
      debug = extension.debug,
      cliExecutable = cliExecutable,
      appBundle = variant.bundle,
      apk = variant.apk,
      sentryOrg = sentryOrg?.let { project.provider { it } } ?: extension.org,
      sentryProject = sentryProject?.let { project.provider { it } } ?: extension.projectName,
      sentryAuthToken = extension.authToken,
      sentryUrl = extension.url,
      sentryProperties = sentryProps,
      taskSuffix = name.capitalized,
      buildVariant = buildVariant,
    )
  // TODO we can use the listToArtifacts API in AGP 8.3+
  // https://github.com/android/gradle-recipes/tree/agp-8.10/listenToArtifacts
  project.afterEvaluate {
    getBundleTask(project, variant.name)!!.configure { it.finalizedBy(uploadBundleTask) }
    getAssembleTaskProvider(project, variant)!!.configure { it.finalizedBy(uploadApkTask) }
  }
  return uploadBundleTask to uploadApkTask
}

private fun <T : InstrumentationParameters> Variant.configureInstrumentation(
  classVisitorFactoryImplClass: Class<out AsmClassVisitorFactory<T>>,
  scope: InstrumentationScope,
  mode: FramesComputationMode,
  excludes: SetProperty<String>,
  instrumentationParamsConfig: (T) -> Unit,
) {
  configureInstrumentationFor74(
    variant = this,
    classVisitorFactoryImplClass,
    scope,
    mode,
    excludes,
    instrumentationParamsConfig,
  )
}

private fun ApplicationVariant.getReleaseInfo(): ReleaseInfo {
  val applicationId = applicationId.orNull ?: namespace.get()
  var versionName = outputs.firstOrNull()?.versionName?.orNull
  if (versionName.isNullOrEmpty()) {
    versionName = "undefined"
  }
  var versionCode = outputs.firstOrNull()?.versionCode?.orNull
  if (versionCode != null && versionCode < 0) {
    versionCode = null
  }
  return ReleaseInfo(applicationId, versionName, versionCode)
}
