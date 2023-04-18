package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import org.gradle.api.Project

class SourceContext {
    companion object {
        fun register(project: Project, extension: SentryPluginExtension, variant: AndroidVariant, paths: OutputPaths, sourceFiles: List<File>, cliExecutable: String, taskSuffix: String) {
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
                input = paths.sourceDir,
                output = paths.bundleDir,
                cliExecutable,
                taskSuffix
            )

            UploadSourceBundleTask.register(
                project,
                variant,
                bundleSourcesTask,
                cliExecutable,
                extension.autoUploadSourceBundle,
                taskSuffix
            )

            WriteBundleIdToManifestTask.register(
                project,
                generateBundleIdTask,
                taskSuffix
            )
        }
    }
}
