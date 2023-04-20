package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import io.sentry.android.gradle.SentryPlugin.Companion.sep
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.SentryTasksProvider.getLintVitalAnalyzeProvider
import io.sentry.android.gradle.SentryTasksProvider.getLintVitalReportProvider
import io.sentry.android.gradle.SentryTasksProvider.getMergeAssetsProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadNativeSymbolsTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.AgpVersions.isAGP74
import io.sentry.android.gradle.util.ReleaseInfo
import io.sentry.android.gradle.util.SentryPluginUtils.isMinificationEnabled
import io.sentry.android.gradle.util.SentryPluginUtils.isVariantAllowed
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import io.sentry.android.gradle.util.hookWithAssembleTasks
import io.sentry.android.gradle.util.hookWithMinifyTasks
import io.sentry.android.gradle.util.hookWithPackageTasks
import io.sentry.android.gradle.util.info
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun AppExtension.configure(
    project: Project,
    extension: SentryPluginExtension,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    applicationVariants.matching {
        isVariantAllowed(extension, it.name, it.flavorName, it.buildType.name)
    }.configureEach { variant ->
        val mergeAssetsDependants = setOf(
            getMergeAssetsProvider(variant),
            // lint vital tasks scan the entire "build" folder; since we're writing our
            // generated stuff in there, we put explicit dependency on them to avoid
            // warnings about implicit dependency
            withLogging(project.logger, "lintVitalAnalyzeTask") {
                getLintVitalAnalyzeProvider(project, variant.name)
            },
            withLogging(project.logger, "lintVitalReportTask") {
                getLintVitalReportProvider(project, variant.name)
            }
        )

        variant.configureDependenciesTask(project, extension, this, mergeAssetsDependants)

        variant.configureProguardMappingsTasks(
            project,
            extension,
            this,
            mergeAssetsDependants,
            cliExecutable,
            sentryOrg,
            sentryProject
        )

        variant.configureNativeSymbolsTask(
            project,
            extension,
            cliExecutable,
            sentryOrg,
            sentryProject
        )
    }
}

private fun BaseVariant.configureDependenciesTask(
    project: Project,
    extension: SentryPluginExtension,
    appExtension: AppExtension,
    dependants: Set<TaskProvider<out Task>?>
) {
    if (isAGP74) {
        project.logger.info {
            "Not configuring deprecated AppExtension for ${AgpVersions.CURRENT}, " +
                "new AppComponentsExtension will be configured"
        }
    } else if (extension.includeDependenciesReport.get()) {
        val outputDir = project.layout.buildDirectory.dir(
            "generated${sep}assets${sep}sentry${sep}dependencies${sep}$name"
        )

        val reportDependenciesTask =
            SentryExternalDependenciesReportTaskFactory.register(
                project = project,
                configurationName = "${name}RuntimeClasspath",
                attributeValueJar = "android-classes",
                includeReport = extension.includeDependenciesReport,
                output = outputDir,
                taskSuffix = name.capitalized
            )
        reportDependenciesTask.setupMergeAssetsDependencies(dependants)
        appExtension.sourceSets.getByName(name).assets.srcDir(
            reportDependenciesTask.flatMap { it.output }
        )
    }
}

private fun ApplicationVariant.configureProguardMappingsTasks(
    project: Project,
    extension: SentryPluginExtension,
    appExtension: AppExtension,
    dependants: Set<TaskProvider<out Task>?>,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    if (isAGP74) {
        project.logger.info {
            "Not configuring deprecated AppExtension for ${AgpVersions.CURRENT}, " +
                "new AppComponentsExtension will be configured"
        }
    } else {
        val variant = AndroidVariant70(this)
        val sentryProps = getPropertiesFilePath(project, variant)
        val guardsquareEnabled = extension.experimentalGuardsquareSupport.get()
        val isMinifyEnabled = isMinificationEnabled(project, variant, guardsquareEnabled)
        val outputDir = project.layout.buildDirectory.dir(
            "generated${sep}assets${sep}sentry${sep}proguard${sep}$name"
        )

        if (isMinifyEnabled && extension.includeProguardMapping.get()) {
            val generateUuidTask =
                SentryGenerateProguardUuidTask.register(
                    project = project,
                    output = outputDir,
                    taskSuffix = name.capitalized
                )
            generateUuidTask.setupMergeAssetsDependencies(dependants)
            generateUuidTask.hookWithPackageTasks(project, variant)
            appExtension.sourceSets.getByName(name).assets.srcDir(
                generateUuidTask.flatMap { it.output }
            )

            val releaseInfo = ReleaseInfo(applicationId, versionName, versionCode)
            val uploadMappingsTask = SentryUploadProguardMappingsTask.register(
                project = project,
                cliExecutable = cliExecutable,
                generateUuidTask = generateUuidTask,
                sentryProperties = sentryProps,
                mappingFiles = SentryTasksProvider.getMappingFileProvider(
                    project,
                    variant,
                    guardsquareEnabled
                ),
                autoUploadProguardMapping = extension.autoUploadProguardMapping,
                sentryOrg = sentryOrg,
                sentryProject = sentryProject,
                taskSuffix = name.capitalized,
                releaseInfo = releaseInfo,
            )
            uploadMappingsTask.hookWithMinifyTasks(project, name, guardsquareEnabled)
        }
    }
}

private fun ApplicationVariant.configureNativeSymbolsTask(
    project: Project,
    extension: SentryPluginExtension,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    // only debug symbols of non debuggable code should be uploaded (aka release builds).
    // uploadSentryNativeSymbols task will only be executed after the assemble task
    // and also only if `uploadNativeSymbols` is enabled, as this is an opt-in feature.
    if (!buildType.isDebuggable && extension.uploadNativeSymbols.get()) {
        val variant = AndroidVariant70(this)
        val sentryProps = getPropertiesFilePath(project, variant)

        // Setup the task to upload native symbols task after the assembling task
        val uploadSentryNativeSymbolsTask = project.tasks.register(
            "uploadSentryNativeSymbolsFor${name.capitalized}",
            SentryUploadNativeSymbolsTask::class.java
        ) {
            it.workingDir(project.rootDir)
            it.autoUploadNativeSymbol.set(extension.autoUploadNativeSymbols)
            it.cliExecutable.set(cliExecutable)
            it.sentryProperties.set(sentryProps?.let { file -> project.file(file) })
            it.includeNativeSources.set(extension.includeNativeSources)
            it.variantName.set(name)
            it.sentryOrganization.set(sentryOrg)
            it.sentryProject.set(sentryProject)
        }
        uploadSentryNativeSymbolsTask.hookWithAssembleTasks(project, variant)
    } else {
        project.logger.info { "uploadSentryNativeSymbols won't be executed" }
    }
}

private fun TaskProvider<out Task>.setupMergeAssetsDependencies(
    dependants: Set<TaskProvider<out Task>?>
) {
    dependants.forEach {
        it?.configure { task ->
            task.dependsOn(this)
        }
    }
}
