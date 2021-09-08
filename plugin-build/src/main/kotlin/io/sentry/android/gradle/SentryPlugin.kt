package io.sentry.android.gradle

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.utils.setDisallowChanges
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
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
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
import org.gradle.api.tasks.TaskProvider

@Suppress("UnstableApiUsage")
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
            val androidComponentsExtension =
                project.extensions.getByType(AndroidComponentsExtension::class.java)
            val cliExecutable = getSentryCliPath(project)

            val extraProperties = project.extensions.getByName("ext")
                as ExtraPropertiesExtension

            val sentryOrgParameter = runCatching {
                extraProperties.get(SENTRY_ORG_PARAMETER).toString()
            }.getOrNull()
            val sentryProjectParameter = runCatching {
                extraProperties.get(SENTRY_PROJECT_PARAMETER).toString()
            }.getOrNull()

            // temp folder for sentry-related stuff
            val tempDir = File("${project.buildDir}${sep}tmp${sep}sentry")
            tempDir.mkdirs()

            // TODO: this should depend on ignoredVariants/ignoredFlavours/ignoredBuildTypes
            androidComponentsExtension.onVariants { variant ->
                variant.transformClassesWith(
                    SpanAddingClassVisitorFactory::class.java,
                    InstrumentationScope.ALL
                ) { params ->
                    if (extension.forceInstrumentDependencies.get()) {
                        params.invalidate.setDisallowChanges(System.currentTimeMillis())
                    }
                    params.debug.setDisallowChanges(extension.debug.get())
                    params.tmpDir.set(tempDir)
                }
                variant.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
            }

            androidExtension.applicationVariants.matching {
                it.name !in extension.ignoredVariants.get() &&
                    it.flavorName !in extension.ignoredFlavors.get() &&
                    it.buildType.name !in extension.ignoredBuildTypes.get()
            }.configureEach { variant ->
                val bundleTask = withLogging(project.logger, "bundleTask") {
                    getBundleTask(project, variant.name)
                }

                val sentryProperties = getPropertiesFilePath(project, variant)

                val isMinifyEnabled = variant.buildType.isMinifyEnabled
                val isDebuggable = variant.buildType.isDebuggable

                var preBundleTaskProvider: TaskProvider<Task>? = null
                var transformerTaskProvider: TaskProvider<Task>? = null
                var packageBundleTaskProvider: TaskProvider<Task>? = null

                if (isMinifyEnabled) {
                    preBundleTaskProvider = withLogging(project.logger, "preBundleTask") {
                        getPreBundleTask(project, variant.name)
                    }
                    transformerTaskProvider = withLogging(project.logger, "transformerTask") {
                        getTransformerTask(project, variant.name)
                    }
                    packageBundleTaskProvider = withLogging(project.logger, "packageBundleTask") {
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
                        File(
                            project.buildDir,
                            "generated${sep}assets${sep}sentry${sep}${variant.name}"
                        )
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
                    transformerTaskProvider?.configure {
                        it.finalizedBy(uploadSentryProguardMappingsTask)
                    }

                    // To include proguard uuid file into aab, run before bundle task.
                    preBundleTaskProvider?.configure { task ->
                        task.dependsOn(uploadSentryProguardMappingsTask)
                    }

                    // The package task will only be executed if the uploadSentryProguardMappingsTask has already been executed.
                    getPackageProvider(variant)?.configure { task ->
                        task.dependsOn(uploadSentryProguardMappingsTask)
                    }
                    // App bundle has different package task
                    packageBundleTaskProvider?.configure { task ->
                        task.dependsOn(uploadSentryProguardMappingsTask)
                    }
                }

                // only debug symbols of non debuggable code should be uploaded (aka release builds).
                // uploadSentryNativeSymbols task will only be executed after the assemble task
                // and also only if `uploadNativeSymbols` is enabled, as this is an opt-in feature.
                if (!isDebuggable && extension.uploadNativeSymbols.get()) {
                    // Setup the task to upload native symbols task after the assembling task
                    val uploadSentryNativeSymbolsTask = project.tasks.register(
                        "uploadSentryNativeSymbolsFor$taskSuffix",
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
                            uploadSentryNativeSymbolsTask
                        )
                    }
                    // if its a bundle aab, assemble might not be executed, so we hook into bundle task
                    bundleTask?.configure { it.finalizedBy(uploadSentryNativeSymbolsTask) }
                } else {
                    project.logger.info("[sentry] uploadSentryNativeSymbols won't be executed")
                }
            }
        }
    }

    companion object {
        const val SENTRY_ORG_PARAMETER = "sentryOrg"
        const val SENTRY_PROJECT_PARAMETER = "sentryProject"
    }
}
