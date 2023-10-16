@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.utils.setDisallowChanges
import io.sentry.android.gradle.SentryPlugin.Companion.sep
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.SentryTasksProvider.getMappingFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.services.SentryModulesService
import io.sentry.android.gradle.sourcecontext.OutputPaths
import io.sentry.android.gradle.sourcecontext.SourceContext
import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.tasks.PropertiesFileOutputTask
import io.sentry.android.gradle.tasks.SentryGenerateDebugMetaPropertiesTask
import io.sentry.android.gradle.tasks.SentryGenerateIntegrationListTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory
import io.sentry.android.gradle.transforms.MetaInfStripTransform
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.AgpVersions.isAGP74
import io.sentry.android.gradle.util.ReleaseInfo
import io.sentry.android.gradle.util.SentryPluginUtils.isMinificationEnabled
import io.sentry.android.gradle.util.SentryPluginUtils.isVariantAllowed
import io.sentry.android.gradle.util.collectModules
import io.sentry.android.gradle.util.hookWithAssembleTasks
import io.sentry.android.gradle.util.hookWithMinifyTasks
import io.sentry.android.gradle.util.info
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.build.event.BuildEventsListenerRegistry

fun AndroidComponentsExtension<*, *, *>.configure(
    project: Project,
    extension: SentryPluginExtension,
    listenerRegistry: BuildEventsListenerRegistry,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    // temp folder for sentry-related stuff
    val tmpDir = File("${project.buildDir}${sep}tmp${sep}sentry")
    tmpDir.mkdirs()

    configureVariants { variant ->
        if (isVariantAllowed(extension, variant.name, variant.flavorName, variant.buildType)) {
            val paths = OutputPaths(project, variant.name)

            variant.configureDependenciesTask(project, extension)

            val tasksGeneratingProperties =
                mutableListOf<TaskProvider<out PropertiesFileOutputTask>>()
            val sourceContextTasks = variant.configureSourceBundleTasks(
                project,
                extension,
                paths,
                cliExecutable,
                sentryOrg,
                sentryProject
            )
            sourceContextTasks?.let { tasksGeneratingProperties.add(it.generateBundleIdTask) }

            val generateProguardUuidTask = variant.configureProguardMappingsTasks(
                project,
                extension,
                paths,
                cliExecutable,
                sentryOrg,
                sentryProject
            )
            generateProguardUuidTask?.let { tasksGeneratingProperties.add(it) }

            variant.configureDebugMetaPropertiesTask(project, tasksGeneratingProperties)

            if (extension.tracingInstrumentation.enabled.get()) {
                /**
                 * We detect sentry-android SDK version using configurations.incoming.afterResolve.
                 * This is guaranteed to be executed BEFORE any of the build tasks/transforms are started.
                 *
                 * After detecting the sdk state, we use Gradle's shared build service to persist
                 * the state between builds and also during a single build, because transforms
                 * are run in parallel.
                 */
                val sentryModulesService = SentryModulesService.register(
                    project,
                    extension.tracingInstrumentation.features,
                    extension.tracingInstrumentation.logcat.enabled,
                    extension.includeSourceContext
                )
                /**
                 * We have to register SentryModulesService as a build event listener, so it will
                 * not be discarded after the configuration phase (where we store the collected
                 * dependencies), and will be passed down to the InstrumentationFactory
                 */
                listenerRegistry.onTaskCompletion(sentryModulesService)

                project.collectModules(
                    "${variant.name}RuntimeClasspath",
                    variant.name,
                    sentryModulesService
                )

                variant.configureInstrumentation(
                    SpanAddingClassVisitorFactory::class.java,
                    InstrumentationScope.ALL,
                    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
                ) { params ->
                    if (extension.tracingInstrumentation.forceInstrumentDependencies.get()) {
                        params.invalidate.setDisallowChanges(System.currentTimeMillis())
                    }
                    params.debug.setDisallowChanges(
                        extension.tracingInstrumentation.debug.get()
                    )
                    params.logcatMinLevel.setDisallowChanges(
                        extension.tracingInstrumentation.logcat.minLevel
                    )
                    params.sentryModulesService.setDisallowChanges(sentryModulesService)
                    params.tmpDir.set(tmpDir)
                }

                val manifestUpdater = project.tasks.register(
                    "${variant.name}SentryGenerateIntegrationListTask",
                    SentryGenerateIntegrationListTask::class.java
                ) {
                    it.sentryModulesService.set(sentryModulesService)
                    it.usesService(sentryModulesService)
                }

                variant.artifacts.use(manifestUpdater).wiredWithFiles(
                    SentryGenerateIntegrationListTask::mergedManifest,
                    SentryGenerateIntegrationListTask::updatedManifest
                ).toTransform(SingleArtifact.MERGED_MANIFEST)

                /**
                 * This necessary to address the issue when target app uses a multi-release jar
                 * (MR-JAR) as a dependency. https://github.com/getsentry/sentry-android-gradle-plugin/issues/256
                 *
                 * We register a transform (https://docs.gradle.org/current/userguide/artifact_transforms.html)
                 * that will strip-out unnecessary files from the MR-JAR, so the AGP transforms
                 * will consume corrected artifacts. We only do this when auto-instrumentation is
                 * enabled (otherwise there's no need in this fix) AND when AGP version
                 * is below 7.1.2, where this issue has been fixed.
                 * (https://androidstudio.googleblog.com/2022/02/android-studio-bumblebee-202111-patch-2.html)
                 */
                if (AgpVersions.CURRENT < AgpVersions.VERSION_7_1_2) {
                    // we are only interested in runtime configuration (as ASM transform is
                    // also run just for the runtime configuration)
                    project.configurations.named("${variant.name}RuntimeClasspath")
                        .configure {
                            it.attributes.attribute(MetaInfStripTransform.metaInfStripped, true)
                        }
                    MetaInfStripTransform.register(
                        project.dependencies,
                        extension.tracingInstrumentation.forceInstrumentDependencies.get()
                    )
                }
            }
        }
    }
}

private fun Variant.configureDebugMetaPropertiesTask(
    project: Project,
    tasksGeneratingProperties: List<TaskProvider<out PropertiesFileOutputTask>>
) {
    if (isAGP74) {
        val taskSuffix = name.capitalized
        val generateDebugMetaPropertiesTask = SentryGenerateDebugMetaPropertiesTask.register(
            project,
            tasksGeneratingProperties,
            null,
            taskSuffix
        )

        configureGeneratedSourcesFor74(
            this,
            generateDebugMetaPropertiesTask to DirectoryOutputTask::output
        )
    } else {
        project.logger.info {
            "Not configuring AndroidComponentsExtension for ${AgpVersions.CURRENT}, since it does" +
                "not have new addGeneratedSourceDirectory API"
        }
    }
}

private fun Variant.configureSourceBundleTasks(
    project: Project,
    extension: SentryPluginExtension,
    paths: OutputPaths,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
): SourceContext.SourceContextTasks? {
    if (extension.includeSourceContext.get()) {
        if (isAGP74) {
            val taskSuffix = name.capitalized
            val variant = AndroidVariant74(this)

            val sourceContextTasks = SourceContext.register(
                project,
                extension,
                variant,
                paths,
                cliExecutable,
                sentryOrg,
                sentryProject,
                taskSuffix
            )

            if (variant.buildTypeName == "release") {
                sourceContextTasks.uploadSourceBundleTask.hookWithAssembleTasks(project, variant)
            }

            return sourceContextTasks
        } else {
            project.logger.info {
                "Not configuring AndroidComponentsExtension for ${AgpVersions.CURRENT}, since it " +
                    "does not have new addGeneratedSourceDirectory API"
            }
            return null
        }
    } else {
        return null
    }
}

private fun Variant.configureDependenciesTask(project: Project, extension: SentryPluginExtension) {
    if (isAGP74) {
        if (extension.includeDependenciesReport.get()) {
            val reportDependenciesTask =
                SentryExternalDependenciesReportTaskFactory.register(
                    project = project,
                    configurationName = "${name}RuntimeClasspath",
                    attributeValueJar = "android-classes",
                    includeReport = extension.includeDependenciesReport,
                    taskSuffix = name.capitalized
                )
            configureGeneratedSourcesFor74(
                variant = this,
                reportDependenciesTask to DirectoryOutputTask::output
            )
        }
    } else {
        project.logger.info {
            "Not configuring AndroidComponentsExtension for ${AgpVersions.CURRENT}, since it does" +
                "not have new addGeneratedSourceDirectory API"
        }
    }
}

private fun Variant.configureProguardMappingsTasks(
    project: Project,
    extension: SentryPluginExtension,
    paths: OutputPaths,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
): TaskProvider<SentryGenerateProguardUuidTask>? {
    if (isAGP74) {
        val variant = AndroidVariant74(this)
        val sentryProps = getPropertiesFilePath(project, variant)
        val guardsquareEnabled = extension.experimentalGuardsquareSupport.get()
        val isMinifyEnabled = isMinificationEnabled(project, variant, guardsquareEnabled)

        if (isMinifyEnabled && extension.includeProguardMapping.get()) {
            val generateUuidTask =
                SentryGenerateProguardUuidTask.register(
                    project = project,
                    taskSuffix = name.capitalized,
                    output = paths.proguardUuidDir
                )

            val releaseInfo = getReleaseInfo(project, this)
            val uploadMappingsTask = SentryUploadProguardMappingsTask.register(
                project = project,
                debug = extension.debug,
                cliExecutable = cliExecutable,
                generateUuidTask = generateUuidTask,
                sentryProperties = sentryProps,
                mappingFiles = getMappingFileProvider(project, variant, guardsquareEnabled),
                autoUploadProguardMapping = extension.autoUploadProguardMapping,
                sentryOrg = sentryOrg?.let { project.provider { it } } ?: extension.org,
                sentryProject = sentryProject?.let { project.provider { it } }
                    ?: extension.projectName,
                sentryAuthToken = extension.authToken,
                sentryUrl = extension.url,
                taskSuffix = name.capitalized,
                releaseInfo = releaseInfo
            )
            uploadMappingsTask.hookWithMinifyTasks(project, name, guardsquareEnabled)

            return generateUuidTask
        } else {
            return null
        }
    } else {
        project.logger.info {
            "Not configuring AndroidComponentsExtension for ${AgpVersions.CURRENT}, since it does" +
                "not have new addGeneratedSourceDirectory API"
        }
        return null
    }
}

private fun <T : InstrumentationParameters> Variant.configureInstrumentation(
    classVisitorFactoryImplClass: Class<out AsmClassVisitorFactory<T>>,
    scope: InstrumentationScope,
    mode: FramesComputationMode,
    instrumentationParamsConfig: (T) -> Unit,
) {
    if (isAGP74) {
        configureInstrumentationFor74(
            variant = this,
            classVisitorFactoryImplClass,
            scope,
            mode,
            instrumentationParamsConfig
        )
    } else {
        configureInstrumentationFor70(
            variant = this,
            classVisitorFactoryImplClass,
            scope,
            mode,
            instrumentationParamsConfig
        )
    }
}

/**
 * onVariants method in AGP 7.4.0 has a binary incompatibility with the prior versions, hence we
 * have to distinguish here, although the compatibility sources would look exactly the same.
 */
private fun AndroidComponentsExtension<*, *, *>.configureVariants(callback: (Variant) -> Unit) {
    if (isAGP74) {
        onVariants74(this, callback)
    } else {
        onVariants70(this, callback)
    }
}

private fun getReleaseInfo(project: Project, variant: Variant): ReleaseInfo {
    val appExtension = project.extensions.getByType(AppExtension::class.java)
    var applicationId =
        appExtension.defaultConfig.applicationId ?: appExtension.namespace.toString()
    var versionName = appExtension.defaultConfig.versionName ?: "undefined"
    var versionCode = appExtension.defaultConfig.versionCode
    val flavor = appExtension.productFlavors.find { it.name == variant.flavorName }
    flavor?.applicationId?.let { applicationId = it }
    flavor?.versionName?.let { versionName = it }
    flavor?.versionCode?.let { versionCode = it }
    flavor?.applicationIdSuffix?.let { applicationId += it }
    flavor?.versionNameSuffix?.let { versionName += it }
    return ReleaseInfo(applicationId, versionName, versionCode)
}
