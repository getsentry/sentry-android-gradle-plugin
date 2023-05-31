package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.Project
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
            taskSuffix: String
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
                taskSuffix
            )

            val collectSourcesTask = CollectSourcesTask.register(
                project,
                sourceFiles,
                output = paths.sourceDir,
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
                sentryOrg ?: extension.org.orNull,
                sentryProject ?: extension.project.orNull,
                extension.authToken,
                taskSuffix
            )

            val uploadSourceBundleTask = UploadSourceBundleTask.register(
                project,
                variant,
                bundleSourcesTask,
                extension.debug,
                cliExecutable,
                extension.autoUploadSourceContext,
                sentryOrg ?: extension.org.orNull,
                sentryProject ?: extension.project.orNull,
                extension.authToken,
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
