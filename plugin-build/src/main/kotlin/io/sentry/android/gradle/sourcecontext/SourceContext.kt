package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

class SourceContext {
  companion object {
    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>?,
      variant: SentryVariant?,
      paths: RootOutputPaths,
      bundleId: Provider<FileCollection>,
      sourceFiles: Provider<FileCollection>?,
      cliExecutable: Provider<String>,
      sentryOrg: String?,
      sentryProject: String?,
      taskSuffix: String,
    ): SourceContextTasks {
      val collectSourcesTask =
        CollectSourcesTask.register(
          project,
          extension,
          sentryTelemetryProvider,
          sourceFiles,
          output = paths.sourceDir,
          extension.includeSourceContext,
          taskSuffix,
        )

      val bundleSourcesTask =
        BundleSourcesTask.register(
          project,
          extension,
          sentryTelemetryProvider,
          variant,
          bundleId,
          collectSourcesTask,
          output = paths.bundleDir,
          extension.debug,
          cliExecutable,
          sentryOrg?.let { project.provider { it } } ?: extension.org,
          sentryProject?.let { project.provider { it } } ?: extension.projectName,
          extension.authToken,
          extension.url,
          extension.includeSourceContext,
          taskSuffix,
        )

      val uploadSourceBundleTask =
        UploadSourceBundleTask.register(
          project,
          extension,
          sentryTelemetryProvider,
          variant,
          bundleSourcesTask,
          extension.debug,
          cliExecutable,
          extension.autoUploadSourceContext,
          sentryOrg?.let { project.provider { it } } ?: extension.org,
          sentryProject?.let { project.provider { it } } ?: extension.projectName,
          extension.authToken,
          extension.url,
          extension.includeSourceContext,
          taskSuffix,
        )

      return SourceContextTasks(
        collectSourcesTask,
        bundleSourcesTask,
        uploadSourceBundleTask,
      )
    }
  }

  class SourceContextTasks(
    val collectSourcesTask: TaskProvider<CollectSourcesTask>,
    val bundleSourcesTask: TaskProvider<BundleSourcesTask>,
    val uploadSourceBundleTask: TaskProvider<UploadSourceBundleTask>,
  )
}
