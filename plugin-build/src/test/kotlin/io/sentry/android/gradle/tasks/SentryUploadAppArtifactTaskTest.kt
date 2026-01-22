package io.sentry.android.gradle.tasks

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

  val dummyApkName = "dummy/folder/app.apk"
  val dummyAabName = "dummy/folder/app.aab"

  @Test
  fun `apk args set correctly`() {
    val project = createProject()

    val apkFile = project.apkDirProvider(dummyApkName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkFile)
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("sentry-cli")
    assertThat(args).contains("build")
    assertThat(args).contains("upload")
    assertThatStrings(args).containsEndingWith(dummyApkName)
  }

  @Test
  fun `aab args set correctly`() {
    val project = createProject()

    val aabFile = project.aabFileProvider(dummyAabName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.bundle.set(aabFile)
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("sentry-cli")
    assertThat(args).contains("build")
    assertThat(args).contains("upload")
    assertThatStrings(args).containsEndingWith(dummyAabName)
  }

  @Test
  fun `--log-level=debug is set correctly`() {
    val project = createProject()

    val apkDir = project.apkDirProvider(dummyApkName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.debug.set(true)
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--log-level=debug")
    assertThatStrings(args).containsEndingWith(dummyApkName)
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

    assertThat(task.get().environment).doesNotContainKey("SENTRY_PROPERTIES")
  }

  @Test
  fun `with sentryAuthToken env variable is set correctly`() {
    val project = createProject()
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.sentryAuthToken.set("<token>")
      }

    task.get().setSentryAuthTokenEnv()

    assertThat(task.get().environment["SENTRY_AUTH_TOKEN"].toString()).contains("<token>")
  }

  @Test
  fun `with sentryUrl sets --url`() {
    val project = createProject()
    val apkDir = project.apkDirProvider(dummyApkName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.sentryUrl.set("https://some-host.sentry.io")
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--url")
    assertThat(args).contains("https://some-host.sentry.io")
    assertThatStrings(args).containsEndingWith(dummyApkName)
  }

  @Test
  fun `with sentryOrganization adds --org`() {
    val project = createProject()

    val apkDir = project.apkDirProvider(dummyApkName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.sentryOrganization.set("dummy-org")
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--org")
    assertThat(args).contains("dummy-org")
  }

  @Test
  fun `with sentryProject adds --project`() {
    val project = createProject()

    val apkDir = project.apkDirProvider(dummyApkName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadProguardMapping", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.sentryProject.set("dummy-proj")
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--project")
    assertThat(args).contains("dummy-proj")
  }

  @Test
  fun `throws exception when bundle file does not exist`() {
    val project = createProject()
    val nonExistentBundle = project.layout.buildDirectory.file("nonexistent/bundle.aab")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.bundle.set(nonExistentBundle)
      }

    val exception = assertFailsWith<IllegalStateException> { task.get().computeCommandLineArgs() }

    assertThat(exception.message).startsWith("Bundle file does not exist:")
  }

  @Test
  fun `throws exception when apk directory does not exist`() {
    val project = createProject()
    val nonExistentApkDir = project.layout.buildDirectory.dir("nonexistent/apk")
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(nonExistentApkDir)
      }

    val exception = assertFailsWith<IllegalStateException> { task.get().computeCommandLineArgs() }

    assertThat(exception.message).startsWith("APK directory does not exist:")
  }

  @Test
  fun `throws exception when apk directory exists but contains no apk files`() {
    val project = createProject()
    val emptyApkDir = project.layout.buildDirectory.dir("empty/apk")
    // Create the directory but don't add any APK files
    emptyApkDir.get().asFile.mkdirs()
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(emptyApkDir)
      }

    val exception = assertFailsWith<IllegalStateException> { task.get().computeCommandLineArgs() }

    assertThat(exception.message).startsWith("No APK file exists in directory:")
  }

  @Test
  fun `throws exception when neither bundle nor apk is set`() {
    val project = createProject()
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
      }

    val exception = assertFailsWith<IllegalStateException> { task.get().computeCommandLineArgs() }

    assertThat(exception.message).isEqualTo("No bundle or apk found")
  }

  @Test
  fun `all vcs parameters are passed to CLI correctly`() {
    val project = createProject()
    val apkDir = project.apkDirProvider(dummyApkName)
    val task: TaskProvider<SentryUploadAppArtifactTask> =
      project.tasks.register("testUploadAppArtifact", SentryUploadAppArtifactTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.apk.set(apkDir)
        it.vcsHeadSha.set("abc123def456")
        it.vcsBaseSha.set("def456abc123")
        it.vcsProvider.set("github")
        it.vcsHeadRepoName.set("getsentry/sentry-android-gradle-plugin")
        it.vcsBaseRepoName.set("getsentry/sentry-android-gradle-plugin")
        it.vcsHeadRef.set("feature-branch")
        it.vcsBaseRef.set("main")
        it.vcsPrNumber.set(123)
        it.buildConfiguration.set("debugRelease")
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).containsAtLeast("--head-sha", "abc123def456").inOrder()
    assertThat(args).containsAtLeast("--base-sha", "def456abc123").inOrder()
    assertThat(args).containsAtLeast("--vcs-provider", "github").inOrder()
    assertThat(args)
      .containsAtLeast("--head-repo-name", "getsentry/sentry-android-gradle-plugin")
      .inOrder()
    assertThat(args)
      .containsAtLeast("--base-repo-name", "getsentry/sentry-android-gradle-plugin")
      .inOrder()
    assertThat(args).containsAtLeast("--head-ref", "feature-branch").inOrder()
    assertThat(args).containsAtLeast("--base-ref", "main").inOrder()
    assertThat(args).containsAtLeast("--pr-number", "123").inOrder()
    assertThat(args).containsAtLeast("--build-configuration", "debugRelease").inOrder()
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("com.android.application")
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }

  private fun Project.apkDirProvider(path: String): Provider<Directory> {
    val apkFileProvider = project.layout.buildDirectory.dir(path)
    apkFileProvider.get().asFile.parentFile.mkdirs()
    apkFileProvider.get().asFile.createNewFile()
    return apkFileProvider
  }

  private fun Project.aabFileProvider(path: String): Provider<RegularFile> {
    val aabFile = project.layout.buildDirectory.file(path)
    aabFile.get().asFile.parentFile.mkdirs()
    aabFile.get().asFile.createNewFile()
    return aabFile
  }
}

class StringListSubject
private constructor(metadata: FailureMetadata, private val actual: List<String>?) :
  Subject(metadata, actual) {

  fun containsEndingWith(suffix: String) {
    if (actual == null) {
      failWithActual("expected to contain a string ending with", suffix)
      return
    }

    if (actual.none { it.endsWith(suffix) }) {
      failWithActual("expected to contain a string ending with", suffix)
    }
  }

  companion object {
    fun strings(): Factory<StringListSubject, List<String>> {
      return Factory { metadata, actual -> StringListSubject(metadata, actual) }
    }
  }
}

fun assertThatStrings(values: List<String>): StringListSubject {
  return assertAbout(StringListSubject.strings()).that(values)
}
