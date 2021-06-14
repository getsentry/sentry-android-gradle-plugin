package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryCliProvider.getSentryCliPath
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.getAssembleTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getMappingFileProvider
import io.sentry.android.gradle.SentryTasksProvider.getMergeAssetsProvider
import io.sentry.android.gradle.SentryTasksProvider.getPackageBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageProvider
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getTransformerTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadNativeSymbolsTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension

class SentryPlugin : Plugin<Project> {

    private val sep = File.separator

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "sentry",
            SentryPluginExtension::class.java,
            project
        )
        project.pluginManager.withPlugin("com.android.application") {
            val androidExtension = project.extensions.getByType(AppExtension::class.java)
            val cliExecutable = getSentryCliPath(project)

            val extraProperties = project.extensions.getByName("ext")
                as ExtraPropertiesExtension

            val sentryOrgParameter = runCatching {
                extraProperties.get(SENTRY_ORG_PARAMETER).toString()
            }.getOrNull()
            val sentryProjectParameter = runCatching {
                extraProperties.get(SENTRY_PROJECT_PARAMETER).toString()
            }.getOrNull()

            androidExtension.applicationVariants.configureEach { variant ->

                val bundleTask = withLogging(project.logger, "bundleTask") {
                    getBundleTask(project, variant.name)
                }

                val sentryProperties = getPropertiesFilePath(project, variant)

                val isMinifyEnabled = variant.buildType.isMinifyEnabled
                val isDebuggable = variant.buildType.isDebuggable

                var preBundleTask: Task? = null
                var transformerTask: Task? = null
                var packageBundleTask: Task? = null

                if (isMinifyEnabled) {
                    preBundleTask = withLogging(project.logger, "preBundleTask") {
                        getPreBundleTask(project, variant.name)
                    }
                    transformerTask = withLogging(project.logger, "transformerTask") {
                        getTransformerTask(project, variant.name)
                    }
                    packageBundleTask = withLogging(project.logger, "packageBundleTask") {
                        getPackageBundleTask(project, variant.name)
                    }
                } else {
                    project.logger.info(
                        "[sentry] isMinifyEnabled is false for variant ${variant.name}."
                    )
                }

                val taskSuffix = variant.name.capitalizeUS()

                if (isMinifyEnabled) {
                    // Setup the task to generate a UUID asset file
                    val uuidOutputDirectory = project.file(
                        "build${sep}generated${sep}assets${sep}sentry${sep}${variant.name}"
                    )
                    val generateUuidTask = project.tasks.register(
                        "generateSentryProguardUuid$taskSuffix",
                        SentryGenerateProguardUuidTask::class.java
                    ) {
                        it.outputDirectory.set(uuidOutputDirectory)
                    }
                    getMergeAssetsProvider(variant)?.configure {
                        it.dependsOn(generateUuidTask)
                    }

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
                        it.uuidDirectory.set(uuidOutputDirectory)
                        it.mappingsFiles = getMappingFileProvider(variant)
                        it.autoUpload.set(extension.autoUpload.get())
                        it.sentryOrganization.set(sentryOrgParameter)
                        it.sentryProject.set(sentryProjectParameter)
                    }
                    androidExtension.sourceSets.getByName(variant.name).assets.srcDir(
                        uuidOutputDirectory
                    )

                    // we just hack ourselves into the proguard task's doLast.
                    transformerTask?.finalizedBy(uploadSentryProguardMappingsTask)

                    // To include proguard uuid file into aab, run before bundle task.
                    preBundleTask?.dependsOn(uploadSentryProguardMappingsTask)

                    // The package task will only be executed if the uploadSentryProguardMappingsTask has already been executed.
                    getPackageProvider(variant)?.configure {
                        it.dependsOn(uploadSentryProguardMappingsTask)
                    }
                    // App bundle has different package task
                    packageBundleTask?.dependsOn(uploadSentryProguardMappingsTask)
                }

                // only debug symbols of non debuggable code should be uploaded (aka release builds).
                // uploadNativeSymbolsTask will only be executed after the assemble task
                // and also only if `uploadNativeSymbols` is enabled, as this is an opt-in feature.
                if (!isDebuggable && extension.uploadNativeSymbols.get()) {
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

                    getAssembleTaskProvider(variant)?.configure {
                        it.finalizedBy(
                            uploadNativeSymbolsTask
                        )
                    }
                    // if its a bundle aab, assemble might not be executed, so we hook into bundle task
                    bundleTask?.finalizedBy(uploadNativeSymbolsTask)
                } else {
                    project.logger.info("[sentry] uploadNativeSymbolsTask won't be executed")
                }
            }
        }
    }

    companion object {
        const val SENTRY_ORG_PARAMETER = "sentryOrg"
        const val SENTRY_PROJECT_PARAMETER = "sentryProject"
    }
}
