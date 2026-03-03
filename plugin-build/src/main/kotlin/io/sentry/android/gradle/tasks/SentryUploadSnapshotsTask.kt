package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.asSentryCliExec
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Uploads should not be cached")
abstract class SentryUploadSnapshotsTask : SentryCliExecTask() {

  init {
    group = SENTRY_GROUP
    description = "Uploads snapshots to Sentry"
  }

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val snapshotsPath: DirectoryProperty

  override fun getArguments(args: MutableList<String>) {
    args.add("build")
    args.add("snapshots")
    args.add(snapshotsPath.get().asFile.absolutePath)
  }

  companion object {
    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>?,
      debug: Property<Boolean>,
      cliExecutable: Provider<String>,
      sentryOrg: Provider<String>,
      sentryProject: Provider<String>,
      sentryAuthToken: Property<String>,
      sentryUrl: Property<String>,
      snapshotsPath: DirectoryProperty,
    ): TaskProvider<SentryUploadSnapshotsTask> {
      return project.tasks.register(
        "sentryUploadSnapshots",
        SentryUploadSnapshotsTask::class.java,
      ) { task ->
        task.workingDir(project.rootDir)
        task.debug.set(debug)
        task.cliExecutable.set(cliExecutable)
        task.sentryOrganization.set(sentryOrg)
        task.sentryProject.set(sentryProject)
        task.sentryAuthToken.set(sentryAuthToken)
        task.sentryUrl.set(sentryUrl)
        task.snapshotsPath.set(snapshotsPath)
        task.sentryTelemetryService.set(sentryTelemetryProvider)
        task.asSentryCliExec()
        task.withSentryTelemetry(extension, sentryTelemetryProvider)
      }
    }
  }
}
