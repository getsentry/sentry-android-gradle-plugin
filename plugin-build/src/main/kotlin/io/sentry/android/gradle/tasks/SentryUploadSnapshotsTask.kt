package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.asSentryCliExec
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
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

  @get:Input abstract val appId: Property<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val snapshotsPath: DirectoryProperty

  override fun getArguments(args: MutableList<String>) {
    args.add("build")
    args.add("snapshots")
    args.add("--app-id")
    args.add(appId.get())
    args.add(snapshotsPath.get().asFile.absolutePath)
  }

  companion object {
    fun register(
      project: Project,
      extension: SentryPluginExtension,
      cliExecutable: Provider<String>,
      sentryOrgOverride: String?,
      sentryProjectOverride: String?,
    ): TaskProvider<SentryUploadSnapshotsTask> {
      return project.tasks.register(
        "sentryUploadSnapshots",
        SentryUploadSnapshotsTask::class.java,
      ) { task ->
        task.workingDir(project.rootDir)
        task.debug.set(extension.debug)
        task.cliExecutable.set(cliExecutable)
        task.sentryProperties.set(
          getPropertiesFilePath(project)?.let { file -> project.file(file) }
        )
        task.sentryOrganization.set(
          sentryOrgOverride?.let { project.provider { it } } ?: extension.org
        )
        task.sentryProject.set(
          sentryProjectOverride?.let { project.provider { it } } ?: extension.projectName
        )
        task.sentryAuthToken.set(extension.authToken)
        task.sentryUrl.set(extension.url)
        task.appId.set(extension.snapshots.appId)
        task.snapshotsPath.set(extension.snapshots.path)
        task.asSentryCliExec()
      }
    }
  }
}
