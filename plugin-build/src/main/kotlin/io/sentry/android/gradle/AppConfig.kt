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
import io.sentry.android.gradle.sourcecontext.OutputPaths
import io.sentry.android.gradle.sourcecontext.SourceContext
import io.sentry.android.gradle.tasks.PropertiesFileOutputTask
import io.sentry.android.gradle.tasks.SentryGenerateDebugMetaPropertiesTask
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import io.sentry.android.gradle.tasks.configureNativeSymbolsTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory
import io.sentry.android.gradle.telemetry.SentryTelemetryService
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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal

fun AppExtension.configure(
    project: Project,
    extension: SentryPluginExtension,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?,
    buildEvents: BuildEventListenerRegistryInternal
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

        val sentryTelemetryProvider = variant.configureTelemetry(
            project,
            extension,
            cliExecutable,
            sentryOrg,
            sentryProject,
            buildEvents
        )

        val tasksGeneratingProperties = mutableListOf<TaskProvider<out PropertiesFileOutputTask>>()
        val sourceContextTasks = variant.configureSourceBundleTasks(
            project,
            extension,
            sentryTelemetryProvider,
            cliExecutable,
            sentryOrg,
            sentryProject
        )
        sourceContextTasks?.let { tasksGeneratingProperties.add(it.generateBundleIdTask) }

        variant.configureDependenciesTask(
            project,
            extension,
            sentryTelemetryProvider,
            this,
            mergeAssetsDependants
        )

        val generateProguardUuidTask = variant.configureProguardMappingsTasks(
            project,
            extension,
            sentryTelemetryProvider,
            cliExecutable,
            sentryOrg,
            sentryProject
        )
        generateProguardUuidTask?.let { tasksGeneratingProperties.add(it) }

        // TODO: do this only once, and all other tasks should be SentryVariant.configureSomething
        val sentryVariant = if (isAGP74) null else AndroidVariant70(variant)
        sentryVariant?.configureNativeSymbolsTask(
            project,
            extension,
            sentryTelemetryProvider,
            cliExecutable,
            sentryOrg,
            sentryProject
        )

        variant.configureDebugMetaPropertiesTask(
            project,
            extension,
            this,
            sentryTelemetryProvider,
            mergeAssetsDependants,
            tasksGeneratingProperties
        )
    }
}

private fun ApplicationVariant.configureTelemetry(
    project: Project,
    extension: SentryPluginExtension,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?,
    buildEvents: BuildEventListenerRegistryInternal
): Provider<SentryTelemetryService> {
    val variant = if (isAGP74) null else AndroidVariant70(this)
    val sentryTelemetryProvider = SentryTelemetryService.register(project)

    project.gradle.taskGraph.whenReady {
        sentryTelemetryProvider.get().start(
            SentryTelemetryService.createParameters(
                project,
                variant,
                extension,
                cliExecutable,
                sentryOrg,
                "Android"
            )
        )
        buildEvents.onOperationCompletion(sentryTelemetryProvider)
    }

    return sentryTelemetryProvider
}

private fun ApplicationVariant.configureDebugMetaPropertiesTask(
    project: Project,
    extension: SentryPluginExtension,
    appExtension: AppExtension,
    sentryTelemetryProvider: Provider<SentryTelemetryService>,
    dependants: Set<TaskProvider<out Task>?>,
    tasksGeneratingProperties: List<TaskProvider<out PropertiesFileOutputTask>>
) {
    if (isAGP74) {
        project.logger.info {
            "Not configuring deprecated AppExtension for ${AgpVersions.CURRENT}, " +
                "new AppComponentsExtension will be configured"
        }
    } else {
        val variant = AndroidVariant70(this)
        val taskSuffix = name.capitalized
        val outputDir = project.layout.buildDirectory.dir(
            "generated${sep}assets${sep}sentry${sep}debug-meta-properties${sep}$name"
        )
        val generateDebugMetaPropertiesTask = SentryGenerateDebugMetaPropertiesTask.register(
            project,
            extension,
            sentryTelemetryProvider,
            tasksGeneratingProperties,
            outputDir,
            taskSuffix
        )

        generateDebugMetaPropertiesTask.setupMergeAssetsDependencies(dependants)
        generateDebugMetaPropertiesTask.hookWithPackageTasks(project, variant)
        appExtension.sourceSets.getByName(name).assets.srcDir(
            generateDebugMetaPropertiesTask.flatMap { it.output }
        )
    }
}

private fun ApplicationVariant.configureSourceBundleTasks(
    project: Project,
    extension: SentryPluginExtension,
    sentryTelemetryProvider: Provider<SentryTelemetryService>,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
): SourceContext.SourceContextTasks? {
    if (isAGP74) {
        project.logger.info {
            "Not configuring deprecated AppExtension for ${AgpVersions.CURRENT}, " +
                "new AppComponentsExtension will be configured"
        }
        return null
    } else if (extension.includeSourceContext.get()) {
        val paths = OutputPaths(project, name)
        val variant = AndroidVariant70(this)
        val taskSuffix = name.capitalized

        val sourceContextTasks = SourceContext.register(
            project,
            extension,
            sentryTelemetryProvider,
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
        return null
    }
}

private fun BaseVariant.configureDependenciesTask(
    project: Project,
    extension: SentryPluginExtension,
    sentryTelemetryProvider: Provider<SentryTelemetryService>,
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
                extension,
                sentryTelemetryProvider,
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
    sentryTelemetryProvider: Provider<SentryTelemetryService>,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
): TaskProvider<SentryGenerateProguardUuidTask>? {
    if (isAGP74) {
        project.logger.info {
            "Not configuring deprecated AppExtension for ${AgpVersions.CURRENT}, " +
                "new AppComponentsExtension will be configured"
        }
        return null
    } else {
        val variant = AndroidVariant70(this)
        val sentryProps = getPropertiesFilePath(project, variant)
        val dexguardEnabled = extension.dexguardEnabled.get()
        val isMinifyEnabled = isMinificationEnabled(project, variant, dexguardEnabled)
        val outputDir = project.layout.buildDirectory.dir(
            "generated${sep}assets${sep}sentry${sep}proguard${sep}$name"
        )

        if (isMinifyEnabled && extension.includeProguardMapping.get()) {
            val generateUuidTask =
                SentryGenerateProguardUuidTask.register(
                    project = project,
                    extension,
                    sentryTelemetryProvider,
                    output = outputDir,
                    taskSuffix = name.capitalized
                )

            val versionName = versionName ?: "undefined"
            val versionCode = if (versionCode == -1) {
                null
            } else {
                versionCode
            }
            val releaseInfo = ReleaseInfo(applicationId, versionName, versionCode)
            val uploadMappingsTask = SentryUploadProguardMappingsTask.register(
                project = project,
                extension,
                sentryTelemetryProvider,
                debug = extension.debug,
                cliExecutable = cliExecutable,
                generateUuidTask = generateUuidTask,
                sentryProperties = sentryProps,
                mappingFiles = SentryTasksProvider.getMappingFileProvider(
                    project,
                    variant,
                    dexguardEnabled
                ),
                autoUploadProguardMapping = extension.autoUploadProguardMapping,
                sentryOrg = sentryOrg?.let { project.provider { it } } ?: extension.org,
                sentryProject = sentryProject?.let { project.provider { it } }
                    ?: extension.projectName,
                sentryAuthToken = extension.authToken,
                taskSuffix = name.capitalized,
                releaseInfo = releaseInfo,
                sentryUrl = extension.url
            )
            uploadMappingsTask.hookWithMinifyTasks(project, name, dexguardEnabled)

            return generateUuidTask
        } else {
            return null
        }
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
