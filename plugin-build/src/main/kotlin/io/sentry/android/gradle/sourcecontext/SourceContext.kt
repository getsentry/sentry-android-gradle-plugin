package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

class SourceContext {
    companion object {
        fun register(project: Project, extension: SentryPluginExtension, variant: AndroidVariant, paths: OutputPaths, sourceFiles: List<File>, cliExecutable: String, taskSuffix: String): SourceContextTasks {
            val generateBundleIdTask = project.tasks.named(GenerateBundleIdTask.taskName(taskSuffix)) as TaskProvider<GenerateBundleIdTask>

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
                input = paths.sourceDir,
                output = paths.bundleDir,
                cliExecutable,
                taskSuffix
            )

            val uploadSourceBundleTask = UploadSourceBundleTask.register(
                project,
                variant,
                bundleSourcesTask,
                cliExecutable,
                extension.autoUploadSourceBundle,
                taskSuffix
            )

            return SourceContextTasks(
                generateBundleIdTask,
                collectSourcesTask,
                bundleSourcesTask,
                uploadSourceBundleTask
//                writeBundleIdToManifestTask
            )
        }
    }

    class SourceContextTasks(
        val generateBundleIdTask: TaskProvider<GenerateBundleIdTask>,
        val collectSourcesTask: TaskProvider<CollectSourcesTask>,
        val bundleSourcesTask: TaskProvider<BundleSourcesTask>,
        val uploadSourceBundleTask: TaskProvider<UploadSourceBundleTask>
//        val writeBundleIdToManifestTask: TaskProvider<WriteBundleIdToManifestTask>
    )
}
