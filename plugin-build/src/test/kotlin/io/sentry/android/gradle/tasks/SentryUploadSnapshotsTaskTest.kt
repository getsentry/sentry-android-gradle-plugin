package io.sentry.android.gradle.tasks

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryUploadSnapshotsTaskTest {

  @Test
  fun `cli-executable is set correctly`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
    }

    val args = task.computeCommandLineArgs()

    assertTrue("sentry-cli" in args)
    assertTrue("build" in args)
    assertTrue("snapshots" in args)
    assertTrue("--app-id" in args)
    assertTrue("com.example" in args)
    assertTrue("/path/to/snapshots" in args)
    assertFalse("--log-level=debug" in args)
  }

  @Test
  fun `--log-level=debug is set correctly`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
      it.debug.set(true)
    }

    val args = task.computeCommandLineArgs()

    assertTrue("--log-level=debug" in args)
  }

  @Test
  fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
    val project = createProject()
    val propertiesFile = project.file("dummy/folder/sentry.properties")
    val task = createTestTask(project) { it.sentryProperties.set(propertiesFile) }

    task.setSentryPropertiesEnv()

    assertEquals(propertiesFile.absolutePath, task.environment["SENTRY_PROPERTIES"].toString())
  }

  @Test
  fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
    val task = createTestTask()

    task.setSentryPropertiesEnv()

    assertNull(task.environment["SENTRY_PROPERTIES"])
  }

  @Test
  fun `with sentryOrganization adds --org`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.sentryOrganization.set("dummy-org")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
    }

    val args = task.computeCommandLineArgs()

    assertTrue("--org" in args)
    assertTrue("dummy-org" in args)
  }

  @Test
  fun `with sentryProject adds --project`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.sentryProject.set("dummy-proj")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
    }

    val args = task.computeCommandLineArgs()

    assertTrue("--project" in args)
    assertTrue("dummy-proj" in args)
  }

  @Test
  fun `with sentryUrl adds --url`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.sentryUrl.set("https://some-host.sentry.io")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
    }

    val args = task.computeCommandLineArgs()

    assertTrue("--url" in args)
    assertTrue("https://some-host.sentry.io" in args)
  }

  @Test
  fun `the --url parameter is placed as the first argument`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.sentryUrl.set("https://some-host.sentry.io")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
    }

    val args = task.computeCommandLineArgs()

    assertEquals(1, args.indexOf("--url"))
  }

  @Test
  fun `all vcs parameters are passed to CLI correctly`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
      it.vcsHeadSha.set("abc123def456")
      it.vcsBaseSha.set("def456abc123")
      it.vcsProvider.set("github")
      it.vcsHeadRepoName.set("getsentry/sentry-android-gradle-plugin")
      it.vcsBaseRepoName.set("getsentry/sentry-android-gradle-plugin")
      it.vcsHeadRef.set("feature-branch")
      it.vcsBaseRef.set("main")
      it.vcsPrNumber.set(123)
    }

    val args = task.computeCommandLineArgs()

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
  }

  @Test
  fun `vcs parameters are omitted when not set`() {
    val task = createTestTask {
      it.cliExecutable.set("sentry-cli")
      it.appId.set("com.example")
      it.snapshotsPath.set(File("/path/to/snapshots"))
    }

    val args = task.computeCommandLineArgs()

    assertThat(args).doesNotContain("--head-sha")
    assertThat(args).doesNotContain("--base-sha")
    assertThat(args).doesNotContain("--vcs-provider")
    assertThat(args).doesNotContain("--head-repo-name")
    assertThat(args).doesNotContain("--base-repo-name")
    assertThat(args).doesNotContain("--head-ref")
    assertThat(args).doesNotContain("--base-ref")
    assertThat(args).doesNotContain("--pr-number")
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }

  private fun createTestTask(
    project: Project = createProject(),
    block: (SentryUploadSnapshotsTask) -> Unit = {},
  ): SentryUploadSnapshotsTask =
    project.tasks
      .register("testUploadSnapshots", SentryUploadSnapshotsTask::class.java) { block(it) }
      .get()
}
