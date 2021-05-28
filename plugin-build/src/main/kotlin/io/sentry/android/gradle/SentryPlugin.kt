package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.Sentry
import io.sentry.android.gradle.SentryCliProvider.getSentryCliPath
import io.sentry.android.gradle.SentryMappingFileProvider.getMappingFile
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getDexTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageTask
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getTransformerTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadNativeSymbolsTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import io.sentry.android.gradle.util.capitalizeUS
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.util.GradleVersion

class SentryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        Sentry.init {
            it.dsn = "https://1053864c67cc410aa1ffc9701bd6f93d@o447951.ingest.sentry.io/5428559"
            // read release dynamically somehow
            it.release = "io.sentry.android.gradle@2.0.0-beta.2-SNAPSHOT"
            it.addInAppInclude("io.sentry.android.gradle")
        }

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
                Sentry.setTag("isMinifyEnabled", isMinifyEnabled.toString())
                Sentry.setTag("gradleVersion", GradleVersion.current().version)
//                Sentry.setTag("agpVersion", how?)

                var dexTask: Task? = null
                var preBundleTask: Task? = null
                var transformerTask: Task? = null
                var packageTask: Task? = null
                var mappingFile: File? = null
                val sep = File.separator

                if (isMinifyEnabled) {
                    dexTask = withLogging(project.logger, "dexTask") {
                        getDexTask(project, variant.name)
                    }
                    preBundleTask = withLogging(project.logger, "preBundleTask") {
                        getPreBundleTask(project, variant.name)
                    }
                    transformerTask = withLogging(project.logger, "transformerTask") {
                        getTransformerTask(project, variant.name)
                    }
                    packageTask = withLogging(project.logger, "packageTask") {
                        getPackageTask(project, variant.name)
                    }
                    mappingFile = getMappingFile(project, variant)
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
                    SentryTasksProvider.getMergeAssetsProvider(variant)?.configure {
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
                        mappingFile?.let { mapFile ->
                            it.mappingsFile.set(mapFile)
                        }
                        it.autoUpload.set(extension.autoUpload.get())
                        it.sentryOrganization.set(sentryOrgParameter)
                        it.sentryProject.set(sentryProjectParameter)
                    }
                    androidExtension.sourceSets.getByName(variant.name).assets.srcDir(
                        uuidOutputDirectory
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
                    SentryTasksProvider.getAssembleTaskProvider(variant)?.configure {
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
