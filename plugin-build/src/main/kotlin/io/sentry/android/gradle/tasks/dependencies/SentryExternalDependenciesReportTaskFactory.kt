package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.util.GradleVersions
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

object SentryExternalDependenciesReportTaskFactory {

  internal const val SENTRY_DEPENDENCIES_REPORT_OUTPUT = "sentry-external-modules.txt"

  fun register(
    project: Project,
    extension: SentryPluginExtension,
    sentryTelemetryProvider: Provider<SentryTelemetryService>,
    configurationName: String,
    attributeValueJar: String,
    includeReport: Provider<Boolean>,
    output: Provider<Directory>? = null,
    taskSuffix: String = "",
  ): TaskProvider<out DirectoryOutputTask> {
    // gradle 7.5 supports passing configuration resolution as task input and respects config
    // cache, so we have a different implementation from that version onwards
    return if (GradleVersions.CURRENT >= GradleVersions.VERSION_7_5) {
      SentryExternalDependenciesReportTaskV2.register(
        project,
        extension,
        sentryTelemetryProvider,
        configurationName,
        attributeValueJar,
        output,
        includeReport,
        taskSuffix,
      )
    } else {
      SentryExternalDependenciesReportTask.register(
        project,
        extension,
        sentryTelemetryProvider,
        configurationName,
        attributeValueJar,
        output,
        includeReport,
        taskSuffix,
      )
    }
  }
}
