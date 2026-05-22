package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.SentryCliProvider
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryCliExecTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `cli path is resolved and extracted from resources`() {
    val project = createProject()
    val cliPath = SentryCliProvider.getCliResourcesExtractionPath(project.buildDir)

    assertTrue(!cliPath.exists())

    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.configureCliPaths(project)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue(cliPath.exists())
    assertEquals(cliPath.absolutePath, File(args[0]).absolutePath)
  }

  @Test
  fun `--log-level=debug is set correctly`() {
    val project = createProject()

    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.configureCliPaths(project)
        it.debug.set(true)
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--log-level=debug" in args)
  }

  @Test
  fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
    val project = createProject()
    val propertiesFile = project.file("dummy/folder/sentry.properties")
    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.sentryProperties.set(propertiesFile)
      }

    task.get().setSentryPropertiesEnv()

    assertEquals(
      propertiesFile.absolutePath,
      task.get().environment["SENTRY_PROPERTIES"].toString(),
    )
  }

  @Test
  fun `with sentryAuthToken env variable is set correctly`() {
    val project = createProject()
    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.sentryAuthToken.set("<token>")
      }

    task.get().setSentryAuthTokenEnv()

    assertEquals("<token>", task.get().environment["SENTRY_AUTH_TOKEN"].toString())
  }

  @Test
  fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
    val project = createProject()
    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java)

    task.get().setSentryPropertiesEnv()

    assertNull(task.get().environment["SENTRY_PROPERTIES"])
  }

  @Test
  fun `with sentryOrganization adds --org`() {
    val project = createProject()
    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.configureCliPaths(project)
        it.sentryOrganization.set("dummy-org")
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--org" in args)
    assertTrue("dummy-org" in args)
  }

  @Test
  fun `with sentryProject adds --project`() {
    val project = createProject()
    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.configureCliPaths(project)
        it.sentryProject.set("dummy-proj")
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--project" in args)
    assertTrue("dummy-proj" in args)
  }

  @Test
  fun `with sentryUrl --url is set`() {
    val project = createProject()
    val task: TaskProvider<TestTask> =
      project.tasks.register("testTask", TestTask::class.java) {
        it.configureCliPaths(project)
        it.sentryUrl.set("https://some-host.sentry.io")
      }

    val args = task.get().computeCommandLineArgs()

    assertTrue("--url" in args)
    assertTrue("https://some-host.sentry.io" in args)
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }

  private fun SentryCliExecTask.configureCliPaths(project: Project) {
    sentryProjectDir.set(project.layout.projectDirectory)
    sentryRootDir.fileValue(project.rootDir)
    buildDirectory.set(project.layout.buildDirectory)
  }

  abstract class TestTask : SentryCliExecTask() {
    override fun getArguments(args: MutableList<String>) {
      // no-op
    }
  }
}
