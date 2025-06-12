package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.asSentryCliExec
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Upload task shouldn't be cached")
abstract class SentryUploadAppArtifactTask @Inject constructor(objectFactory: ObjectFactory) :
  SentryCliExecTask() {

  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  val bundle: Property<RegularFile> = objectFactory.fileProperty()

  @get:InputDirectory
  @get:Optional
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  abstract val apk: DirectoryProperty

  override fun getArguments(args: MutableList<String>) {
    args.add("mobile-app")
    args.add("upload")
    if (bundle.isPresent && bundle.get().asFile.exists()) {
      args.add(bundle.get().asFile.path)
    } else if (apk.isPresent && apk.get().asFile.exists()) {
      // find *.apk inside apk input directory
      val path = apk.get().asFileTree.find { file -> file.name.endsWith(".apk") }!!.path
      args.add(path)
    }
  }

  companion object {
    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>?,
      debug: Property<Boolean>,
      appBundle: Provider<RegularFile>,
      apk: Provider<Directory>,
      cliExecutable: Provider<String>,
      sentryProperties: String?,
      sentryOrg: Provider<String>,
      sentryProject: Provider<String>,
      sentryAuthToken: Property<String>,
      sentryUrl: Property<String>,
      taskSuffix: String = "",
    ): Pair<TaskProvider<SentryUploadAppArtifactTask>, TaskProvider<SentryUploadAppArtifactTask>> {
      val uploadMobileBundleTask =
        project.tasks.register(
          "uploadSentryBundle$taskSuffix",
          SentryUploadAppArtifactTask::class.java,
        ) { task ->
          task.workingDir(project.rootDir)
          task.debug.set(debug)
          task.cliExecutable.set(cliExecutable)
          task.sentryProperties.set(sentryProperties?.let { file -> project.file(file) })
          task.bundle.set(appBundle)
          task.sentryOrganization.set(sentryOrg)
          task.sentryProject.set(sentryProject)
          task.sentryAuthToken.set(sentryAuthToken)
          task.sentryUrl.set(sentryUrl)
          sentryTelemetryProvider?.let { task.sentryTelemetryService.set(it) }
          task.asSentryCliExec()
          task.withSentryTelemetry(extension, sentryTelemetryProvider)
        }
      val uploadMobileApkTask =
        project.tasks.register(
          "uploadSentryApk$taskSuffix",
          SentryUploadAppArtifactTask::class.java,
        ) { task ->
          task.workingDir(project.rootDir)
          task.debug.set(debug)
          task.cliExecutable.set(cliExecutable)
          task.sentryProperties.set(sentryProperties?.let { file -> project.file(file) })
          task.apk.set(apk)
          task.sentryOrganization.set(sentryOrg)
          task.sentryProject.set(sentryProject)
          task.sentryAuthToken.set(sentryAuthToken)
          task.sentryUrl.set(sentryUrl)
          sentryTelemetryProvider?.let { task.sentryTelemetryService.set(it) }
          task.asSentryCliExec()
          task.withSentryTelemetry(extension, sentryTelemetryProvider)
        }
      return uploadMobileBundleTask to uploadMobileApkTask
    }
  }
}
