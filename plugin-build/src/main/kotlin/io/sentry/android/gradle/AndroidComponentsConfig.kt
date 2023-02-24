@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.utils.setDisallowChanges
import io.sentry.android.gradle.SentryPlugin.Companion.sep
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.SentryTasksProvider.getMappingFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.services.SentryModulesService
import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.tasks.SentryGenerateIntegrationListTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.tasks.SentryWriteProguardUUIDToManifestTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory
import io.sentry.android.gradle.transforms.MetaInfStripTransform
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.AgpVersions.isAGP74
import io.sentry.android.gradle.util.SentryPluginUtils.isMinificationEnabled
import io.sentry.android.gradle.util.SentryPluginUtils.isVariantAllowed
import io.sentry.android.gradle.util.collectModules
import io.sentry.android.gradle.util.hookWithMinifyTasks
import io.sentry.android.gradle.util.info
import java.io.File
import org.gradle.api.Project

fun AndroidComponentsExtension<*, *, *>.configure(
    project: Project,
    extension: SentryPluginExtension,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    // temp folder for sentry-related stuff
    val tmpDir = File("${project.buildDir}${sep}tmp${sep}sentry")
    tmpDir.mkdirs()

    configureVariants { variant ->
        if (isVariantAllowed(extension, variant.name, variant.flavorName, variant.buildType)) {
            variant.configureDependenciesTask(project, extension)

            variant.configureProguardMappingsTasks(
                project,
                extension,
                cliExecutable,
                sentryOrg,
                sentryProject
            )

            if (extension.tracingInstrumentation.enabled.get()) {
                /**
                 * We detect sentry-android SDK version using configurations.incoming.afterResolve.
                 * This is guaranteed to be executed BEFORE any of the build tasks/transforms are started.
                 *
                 * After detecting the sdk state, we use Gradle's shared build service to persist
                 * the state between builds and also during a single build, because transforms
                 * are run in parallel.
                 */
                val sentryModulesService = SentryModulesService.register(project)
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
                    params.features.setDisallowChanges(
                        extension.tracingInstrumentation.features.get()
                    )
                    params.sentryModulesService.setDisallowChanges(sentryModulesService)
                    params.tmpDir.set(tmpDir)
                }

                val manifestUpdater = project.tasks.register(
                    "${variant.name}SentryGenerateIntegrationListTask",
                    SentryGenerateIntegrationListTask::class.java
                ) {
                    it.integrations.set(
                        project.objects.listProperty(String::class.java).apply {
                            addAll(extension.tracingInstrumentation.features.get().map { it.name })
                        }
                    )
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
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    if (isAGP74) {
        val variant = AndroidVariant74(this)
        val sentryProps = getPropertiesFilePath(project, variant)
        val guardsquareEnabled = extension.experimentalGuardsquareSupport.get()
        val isMinifyEnabled = isMinificationEnabled(project, variant, guardsquareEnabled)

        if (isMinifyEnabled && extension.includeProguardMapping.get()) {
            val generateUuidTask =
                SentryGenerateProguardUuidTask.register(
                    project = project,
                    taskSuffix = name.capitalized
                )
            configureGeneratedSourcesFor74(
                variant = this,
                generateUuidTask to DirectoryOutputTask::output
            )

            val manifestUpdater = project.tasks.register(
                variant.name + "ManifestUpdater",
                SentryWriteProguardUUIDToManifestTask::class.java
            ) {
                it.gitInfoFile.set(generateUuidTask.flatMap { it.outputFile })
            }
            artifacts.use(manifestUpdater).wiredWithFiles(
                SentryWriteProguardUUIDToManifestTask::mergedManifest,
                SentryWriteProguardUUIDToManifestTask::updatedManifest
            ).toTransform(SingleArtifact.MERGED_MANIFEST)

            val uploadMappingsTask = SentryUploadProguardMappingsTask.register(
                project = project,
                cliExecutable = cliExecutable,
                generateUuidTask = generateUuidTask,
                sentryProperties = sentryProps,
                mappingFiles = getMappingFileProvider(project, variant, guardsquareEnabled),
                autoUploadProguardMapping = extension.autoUploadProguardMapping,
                sentryOrg = sentryOrg,
                sentryProject = sentryProject,
                taskSuffix = name.capitalized
            )
            uploadMappingsTask.hookWithMinifyTasks(project, name, guardsquareEnabled)
        }
    } else {
        project.logger.info {
            "Not configuring AndroidComponentsExtension for ${AgpVersions.CURRENT}, since it does" +
                "not have new addGeneratedSourceDirectory API"
        }
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
