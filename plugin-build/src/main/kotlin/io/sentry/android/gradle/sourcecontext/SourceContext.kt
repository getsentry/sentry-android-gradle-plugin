package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

class SourceContext {
    companion object {
        fun register(
            project: Project,
            extension: SentryPluginExtension,
            variant: SentryVariant,
            paths: OutputPaths,
            cliExecutable: String,
            sentryOrg: String?,
            sentryProject: String?,
            taskSuffix: String,
            sentryTelemetryProvider: Provider<SentryTelemetryService>
        ): SourceContextTasks {
            val additionalSourcesProvider = project.provider {
                extension.additionalSourceDirsForSourceContext.getOrElse(emptySet())
                    .map { project.layout.projectDirectory.dir(it) }
            }
            val sourceFiles = variant.sources(
                project,
                additionalSourcesProvider
            )
            val generateBundleIdTask = GenerateBundleIdTask.register(
                project,
                output = paths.bundleIdDir,
                extension.includeSourceContext,
                sentryTelemetryProvider,
                taskSuffix
            )

            val collectSourcesTask = CollectSourcesTask.register(
                project,
                sourceFiles,
                output = paths.sourceDir,
                extension.includeSourceContext,
                taskSuffix
            )

            val bundleSourcesTask = BundleSourcesTask.register(
                project,
                variant,
                generateBundleIdTask,
                collectSourcesTask,
                output = paths.bundleDir,
                extension.debug,
                cliExecutable,
                sentryOrg?.let { project.provider { it } } ?: extension.org,
                sentryProject?.let { project.provider { it } } ?: extension.projectName,
                extension.authToken,
                extension.includeSourceContext,
                sentryTelemetryProvider,
                taskSuffix
            )

            val uploadSourceBundleTask = UploadSourceBundleTask.register(
                project,
                variant,
                bundleSourcesTask,
                extension.debug,
                cliExecutable,
                extension.autoUploadSourceContext,
                sentryOrg?.let { project.provider { it } } ?: extension.org,
                sentryProject?.let { project.provider { it } } ?: extension.projectName,
                extension.authToken,
                extension.includeSourceContext,
                taskSuffix
            )

            return SourceContextTasks(
                generateBundleIdTask,
                collectSourcesTask,
                bundleSourcesTask,
                uploadSourceBundleTask
            )
        }
    }

    class SourceContextTasks(
        val generateBundleIdTask: TaskProvider<GenerateBundleIdTask>,
        val collectSourcesTask: TaskProvider<CollectSourcesTask>,
        val bundleSourcesTask: TaskProvider<BundleSourcesTask>,
        val uploadSourceBundleTask: TaskProvider<UploadSourceBundleTask>
    )
}
