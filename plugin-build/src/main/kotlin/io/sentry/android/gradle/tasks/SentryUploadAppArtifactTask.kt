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
import org.gradle.api.tasks.Input
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

  @get:Input @get:Optional abstract val vcsHeadSha: Property<String>
  @get:Input @get:Optional abstract val vcsBaseSha: Property<String>
  @get:Input @get:Optional abstract val vcsProvider: Property<String>
  @get:Input @get:Optional abstract val vcsHeadRepoName: Property<String>
  @get:Input @get:Optional abstract val vcsBaseRepoName: Property<String>
  @get:Input @get:Optional abstract val vcsHeadRef: Property<String>
  @get:Input @get:Optional abstract val vcsBaseRef: Property<String>
  @get:Input @get:Optional abstract val vcsPrNumber: Property<Int>
  @get:Input @get:Optional abstract val buildConfiguration: Property<String>

  override fun getArguments(args: MutableList<String>) {
    args.add("build")
    args.add("upload")

    // Add VCS parameters if provided
    vcsHeadSha.orNull?.let { args.addAll(listOf("--head-sha", it)) }
    vcsBaseSha.orNull?.let { args.addAll(listOf("--base-sha", it)) }
    vcsProvider.orNull?.let { args.addAll(listOf("--vcs-provider", it)) }
    vcsHeadRepoName.orNull?.let { args.addAll(listOf("--head-repo-name", it)) }
    vcsBaseRepoName.orNull?.let { args.addAll(listOf("--base-repo-name", it)) }
    vcsHeadRef.orNull?.let { args.addAll(listOf("--head-ref", it)) }
    vcsBaseRef.orNull?.let { args.addAll(listOf("--base-ref", it)) }
    vcsPrNumber.orNull?.let { args.addAll(listOf("--pr-number", it.toString())) }
    buildConfiguration.orNull?.let { args.addAll(listOf("--build-configuration", it)) }

    val bundleFile = bundle.orNull?.asFile
    if (bundleFile != null) {
      if (bundleFile.exists()) {
        args.add(bundleFile.path)
        return
      } else {
        throw IllegalStateException("Bundle file does not exist: ${bundleFile.path}")
      }
    }

    val apkDir = apk.orNull?.asFile
    if (apkDir != null) {
      if (apkDir.exists()) {
        val apkFile = apkDir.walkTopDown().find { it.isFile && it.name.endsWith(".apk") }
        if (apkFile != null) {
          args.add(apkFile.path)
          return
        } else {
          throw IllegalStateException("No APK file exists in directory: ${apkDir.path}")
        }
      } else {
        throw IllegalStateException("APK directory does not exist: ${apkDir.path}")
      }
    }

    throw IllegalStateException("No bundle or apk found")
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
      buildVariant: String = "",
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
          task.vcsHeadSha.set(extension.vcsInfo.headSha)
          task.vcsBaseSha.set(extension.vcsInfo.baseSha)
          task.vcsProvider.set(extension.vcsInfo.vcsProvider)
          task.vcsHeadRepoName.set(extension.vcsInfo.headRepoName)
          task.vcsBaseRepoName.set(extension.vcsInfo.baseRepoName)
          task.vcsHeadRef.set(extension.vcsInfo.headRef)
          task.vcsBaseRef.set(extension.vcsInfo.baseRef)
          task.vcsPrNumber.set(extension.vcsInfo.prNumber)
          task.buildConfiguration.set(
            extension.sizeAnalysis.buildConfiguration.orElse(project.provider { buildVariant })
          )
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
          task.vcsHeadSha.set(extension.vcsInfo.headSha)
          task.vcsBaseSha.set(extension.vcsInfo.baseSha)
          task.vcsProvider.set(extension.vcsInfo.vcsProvider)
          task.vcsHeadRepoName.set(extension.vcsInfo.headRepoName)
          task.vcsBaseRepoName.set(extension.vcsInfo.baseRepoName)
          task.vcsHeadRef.set(extension.vcsInfo.headRef)
          task.vcsBaseRef.set(extension.vcsInfo.baseRef)
          task.vcsPrNumber.set(extension.vcsInfo.prNumber)
          task.buildConfiguration.set(
            extension.sizeAnalysis.buildConfiguration.orElse(project.provider { buildVariant })
          )
          sentryTelemetryProvider?.let { task.sentryTelemetryService.set(it) }
          task.asSentryCliExec()
          task.withSentryTelemetry(extension, sentryTelemetryProvider)
        }
      return uploadMobileBundleTask to uploadMobileApkTask
    }
  }
}
