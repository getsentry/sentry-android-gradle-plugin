package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import io.sentry.android.gradle.SentryCliProvider.getSentryCliPath
import io.sentry.android.gradle.SentryMappingFileProvider.getMappingFile
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.getAssembleTask
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getDexTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageTask
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getTransformerTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadNativeSymbolsTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import java.io.File
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension

class SentryPlugin : Plugin<Project> {
    @OptIn(ExperimentalStdlibApi::class)
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "sentry",
            SentryPluginExtension::class.java,
            project
        )
        project.afterEvaluate {
            check(project.plugins.hasPlugin(AppPlugin::class.java)) {
                "[sentry] Must apply `com.android.application` first!"
            }

            val androidExtension = project.extensions.getByType(AppExtension::class.java)

            fun withLogging(varName: String, initializer: () -> Task?) =
                initializer().also {
                    project.logger.info("[sentry] $varName is ${it?.path}")
                }

            val cliExecutable = getSentryCliPath(project)

            val extraProperties = project.extensions.getByName("ext")
                as ExtraPropertiesExtension

            val sentryOrgParameter = runCatching {
                extraProperties.get(SENTRY_ORG_PARAMETER).toString()
            }.getOrNull()
            val sentryProjectParameter = runCatching {
                extraProperties.get(SENTRY_PROJECT_PARAMETER).toString()
            }.getOrNull()

            androidExtension.applicationVariants.all { variant ->

                val varNameCapitalize = variant.name.capitalize(Locale.ROOT)

                val bundleTask = withLogging("bundleTask") {
                    getBundleTask(project, variant.name)
                }

                val assembleTask = withLogging("assembleTask") { getAssembleTask(variant) }

                val sentryProperties = getPropertiesFilePath(project, variant)

                val isMinifyEnabled = variant.buildType.isMinifyEnabled

                var dexTask: Task? = null
                var preBundleTask: Task? = null
                var transformerTask: Task? = null
                var packageTask: Task? = null
                var mappingFile: File? = null
                val sep = File.separator

                if (isMinifyEnabled) {
                    dexTask = withLogging("dexTask") { getDexTask(project, variant.name) }
                    preBundleTask = withLogging("preBundleTask") {
                        getPreBundleTask(project, variant.name)
                    }
                    transformerTask = withLogging("transformerTask") {
                        getTransformerTask(project, variant.name)
                    }
                    packageTask = withLogging("packageTask") {
                        getPackageTask(project, variant.name)
                    }
                    mappingFile = getMappingFile(project, variant)
                } else {
                    project.logger.info("[sentry] isMinifyEnabled is disabled.")
                }

                variant.outputs.all { variantOutput ->
                    val taskSuffix = varNameCapitalize + variantOutput.name.capitalize(Locale.ROOT)

                    if (isMinifyEnabled) {
                        // Setup the task to generate a UUID asset file
                        val generateUuidTask = project.tasks.register(
                            "generateSentryProguardUuid$taskSuffix",
                            SentryGenerateProguardUuidTask::class.java
                        ) {
                            it.outputDirectory.set(
                                project.file(
                                    "build${sep}generated${sep}assets${sep}sentry${sep}${variant.name}"
                                )
                            )
                        }
                        variant.mergeAssetsProvider.configure { it.dependsOn(generateUuidTask) }

                        // Setup the task that uploads the proguard mapping and UUIDs
                        val uploadSentryProguardMappingsTask = project.tasks.register(
                            "uploadSentryProguardMappings$taskSuffix",
                            SentryUploadProguardMappingsTask::class.java
                        ) {
                            it.dependsOn(generateUuidTask)
                            it.workingDir(project.rootDir)
                            it.cliExecutable.set(cliExecutable)
                            it.sentryProperties.set(
                                sentryProperties?.let { file -> project.file(file) }
                            )
                            it.mappingsUuid.set(generateUuidTask.get().outputUuid)
                            mappingFile?.let { mapFile ->
                                it.mappingsFile.set(mapFile)
                            }
                            it.autoUpload.set(extension.autoUpload.get())
                            it.sentryOrganization.set(sentryOrgParameter)
                            it.sentryProject.set(sentryProjectParameter)
                        }
                        variant.register(uploadSentryProguardMappingsTask.get())
                        androidExtension.sourceSets.getByName(variant.name).assets.srcDir(
                            generateUuidTask.get().outputDirectory
                        )

                        // and run before dex transformation. If we managed to find the dex task
                        // we set ourselves as dependency, otherwise we just hack ourselves into
                        // the proguard task's doLast.
                        dexTask?.dependsOn(uploadSentryProguardMappingsTask)
                        transformerTask?.finalizedBy(uploadSentryProguardMappingsTask)

                        // To include proguard uuid file into aab, run before bundle task.
                        preBundleTask?.dependsOn(uploadSentryProguardMappingsTask)

                        // The package task will only be executed if the uploadSentryProguardMappingsTask has already been executed.
                        packageTask?.dependsOn(uploadSentryProguardMappingsTask)
                    }

                    // Setup the task to upload native symbols task after the assembling task
                    val uploadNativeSymbolsTask = project.tasks.register(
                        "uploadNativeSymbolsFor$taskSuffix",
                        SentryUploadNativeSymbolsTask::class.java
                    ) {
                        it.workingDir(project.rootDir)
                        it.cliExecutable.set(cliExecutable)
                        it.sentryProperties.set(
                            sentryProperties?.let { file -> project.file(file) }
                        )
                        it.includeNativeSources.set(extension.includeNativeSources.get())
                        it.variantName.set(variant.name)
                        it.sentryOrganization.set(sentryOrgParameter)
                        it.sentryProject.set(sentryProjectParameter)
                    }

                    // uploadNativeSymbolsTask will only be executed after the assemble task
                    // and also only if `uploadNativeSymbols` is enabled, as this is an opt-in feature.
                    if (extension.uploadNativeSymbols.get()) {
                        assembleTask?.finalizedBy(uploadNativeSymbolsTask)
                        // if its a bundle aab, assemble might not be executed, so we hook into bundle task
                        bundleTask?.finalizedBy(uploadNativeSymbolsTask)
                    } else {
                        project.logger.info("[sentry] uploadNativeSymbolsTask won't be executed")
                    }
                }
            }
        }
    }

    companion object {
        const val SENTRY_ORG_PARAMETER = "sentryOrg"
        const val SENTRY_PROJECT_PARAMETER = "sentryProject"
    }
}
