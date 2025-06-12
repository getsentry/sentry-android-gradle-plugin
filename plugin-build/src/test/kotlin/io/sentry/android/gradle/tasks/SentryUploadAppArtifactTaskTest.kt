package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.ReleaseInfo
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryUploadAppArtifactTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `cli-executable is set correctly`() {
    val project = createProject()

    val apkFile = createApkDirProvider(project, "dummy/folder/app.apk")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkFile)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("sentry-cli" in args)
    assertTrue("mobile-app" in args)
    assertTrue("upload" in args)
    assertFalse("--no-upload" in args)
  }

  @Test
  fun `with no version code cli-executable is set correctly`() {
    val randomUuid = UUID.randomUUID()
    val project = createProject()
    val releaseInfo = ReleaseInfo("com.test", "1.0.0")

    val apkDir = createApkDirProvider(project, "dummy/folder/mapping.txt")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("sentry-cli" in args)
    assertTrue("upload-proguard" in args)
    assertTrue("--uuid" in args)
    assertTrue(randomUuid.toString() in args)
    assertTrue(apkDir.get().toString() in args)
    assertTrue("--app-id" in args)
    assertTrue(releaseInfo.applicationId in args)
    assertTrue("--version" in args)
    assertTrue(releaseInfo.versionName in args)
    assertFalse("--version-code" in args)
    assertFalse("--no-upload" in args)
    assertFalse("--log-level=debug" in args)
  }

  @Test
  fun `with multiple mappingFiles picks the first existing file`() {
    val project = createProject()

    val apkDir = createApkDirProvider(project, "dummy/folder/missing-mapping.txt")
    val existingFile =
      project.file("dummy/folder/existing-mapping.txt").apply {
        parentFile.mkdirs()
        writeText("dummy-file")
      }

    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue(existingFile.toString() in args, "Observed args: $args")
  }

  @Test
  fun `--auto-upload is set correctly`() {
    val project = createProject()

    val apkDir = createApkDirProvider(project, "dummy/folder/mapping.txt")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--no-upload" in args)
  }

  @Test
  fun `--log-level=debug is set correctly`() {
    val project = createProject()

    val apkDir = createApkDirProvider(project, "dummy/folder/mapping.txt")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.debug.set(true)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--log-level=debug" in args)
  }

  @Test
  fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
    val project = createProject()
    val propertiesFile = project.file("dummy/folder/sentry.properties")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.sentryProperties.set(propertiesFile)
      }

    task.get().setSentryPropertiesEnv()

    assertEquals(
      propertiesFile.absolutePath,
      task.get().environment["SENTRY_PROPERTIES"].toString(),
    )
  }

  @Test
  fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
    val project = createProject()
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java)

    task.get().setSentryPropertiesEnv()

    assertNull(task.get().environment["SENTRY_PROPERTIES"])
  }

  @Test
  fun `with sentryAuthToken env variable is set correctly`() {
    val project = createProject()
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.sentryAuthToken.set("<token>")
      }

    task.get().setSentryAuthTokenEnv()

    assertEquals("<token>", task.get().environment["SENTRY_AUTH_TOKEN"].toString())
  }

  @Test
  fun `with sentryUrl sets --url`() {
    val project = createProject()
    val apkDir = createApkDirProvider(project, "dummy/folder/mapping.txt")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.sentryUrl.set("https://some-host.sentry.io")
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--url" in args)
    assertTrue("https://some-host.sentry.io" in args)
  }

  @Test
  fun `with sentryOrganization adds --org`() {
    val project = createProject()

    val apkDir = createApkDirProvider(project, "dummy/folder/mapping.txt")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.sentryOrganization.set("dummy-org")
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--org" in args)
    assertTrue("dummy-org" in args)
  }

  @Test
  fun `with sentryProject adds --project`() {
    val project = createProject()

    val apkDir = createApkDirProvider(project, "dummy/folder/mapping.txt")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.sentryProject.set("dummy-proj")
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--project" in args)
    assertTrue("dummy-proj" in args)
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }

  private fun createFakeUuid(
    project: Project,
    uuid: UUID = UUID.randomUUID(),
  ): Provider<RegularFile> {
    val file =
      tempDir.newFile("sentry-debug-meta.properties").apply {
        writeText("io.sentry.ProguardUuids=$uuid")
      }
    return project.layout.file(project.provider { file })
  }

  private fun createApkDirProvider(project: Project, path: String): Provider<Directory> =
    project.layout.buildDirectory.dir(path)
}
