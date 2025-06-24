package io.sentry.android.gradle.tasks

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.sourcecontext.BundleSourcesTask
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import java.io.File
import java.util.Properties
import java.util.UUID
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BundleSourcesTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `cli-executable is set correctly`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("sentry-cli")
    assertThat(args).contains("debug-files")
    assertThat(args).contains("bundle-jvm")
    assertThat(args).contains(sourceDir.absolutePath)
    assertThat(args).contains("--output=${outDir.absolutePath}")

    assertThat(args).doesNotContain("--org")
    assertThat(args).doesNotContain("--project")
    assertThat(args).doesNotContain("--log-level=debug")
  }

  @Test
  fun `--log-level=debug is set correctly`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
        it.debug.set(true)
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--log-level=debug")
  }

  @Test
  fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
    val project = createProject()
    val propertiesFile = project.file("dummy/folder/sentry.properties")
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
        it.sentryProperties.set(propertiesFile)
      }

    task.get().setSentryPropertiesEnv()

    assertThat(task.get().environment["SENTRY_PROPERTIES"].toString())
      .isEqualTo(propertiesFile.toString())
  }

  @Test
  fun `with sentryAuthToken env variable is set correctly`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
        it.sentryAuthToken.set("<token>")
      }

    task.get().setSentryAuthTokenEnv()

    assertThat(task.get().environment).containsEntry("SENTRY_AUTH_TOKEN", "<token>")
  }

  @Test
  fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
      }

    task.get().setSentryPropertiesEnv()

    assertThat(task.get().environment).doesNotContainKey("SENTRY_PROPERTIES")
  }

  @Test
  fun `with sentryOrganization adds --org`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
        it.sentryOrganization.set("dummy-org")
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--org")
    assertThat(args).contains("dummy-org")
  }

  @Test
  fun `with sentryProject adds --project`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
        it.sentryProject.set("dummy-proj")
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--project")
    assertThat(args).contains("dummy-proj")
  }

  @Test
  fun `readBundleIdFromFile works correctly`() {
    val expected = "8c776014-bb25-11eb-8529-0242ac130003"
    val input = tempDir.newFile().apply { writeText("$SENTRY_BUNDLE_ID_PROPERTY=$expected") }
    val actual = BundleSourcesTask.readBundleIdFromFile(input)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `readBundleIdFromFile works correctly with whitespaces`() {
    val expected = "8c776014-bb25-11eb-8529-0242ac130003"
    val input = tempDir.newFile().apply { writeText(" $SENTRY_BUNDLE_ID_PROPERTY=$expected\n") }
    val actual = BundleSourcesTask.readBundleIdFromFile(input)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `readBundleIdFromFile fails with missing file`() {
    assertThrows(IllegalStateException::class.java) {
      BundleSourcesTask.readBundleIdFromFile(File("missing"))
    }
  }

  @Test
  fun `readBundleIdFromFile fails with empty file`() {
    assertThrows(IllegalStateException::class.java) {
      BundleSourcesTask.readBundleIdFromFile(tempDir.newFile())
    }
  }

  @Test
  fun `readBundleIdFromFile fails with missing property`() {
    assertThrows(IllegalStateException::class.java) {
      val inputFile = tempDir.newFile().apply { writeText("a.property=true") }
      BundleSourcesTask.readBundleIdFromFile(inputFile)
    }
  }

  @Test
  fun `with sentryUrl --url is set`() {
    val project = createProject()
    val debugMetaPropertiesFile = createDebugMetaProperties(project)

    val sourceDir = File(project.buildDir, "dummy/source")
    val outDir = File(project.buildDir, "dummy/out")
    val task: TaskProvider<BundleSourcesTask> =
      project.tasks.register("testBundleSources", BundleSourcesTask::class.java) {
        it.cliExecutable.set("sentry-cli")
        it.sourceDir.set(sourceDir)
        it.bundleIdFile.set(debugMetaPropertiesFile)
        it.output.set(outDir)
        it.sentryUrl.set("https://some-host.sentry.io")
      }

    val args = task.get().computeCommandLineArgs()

    assertThat(args).contains("--url")
    assertThat(args).contains("https://some-host.sentry.io")
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }

  private fun createDebugMetaProperties(
    project: Project,
    uuid: UUID = UUID.randomUUID(),
  ): Provider<RegularFile> {
    val file =
      tempDir.newFile("sentry-debug-meta.properties").apply {
        Properties().also { props ->
          props.setProperty(SENTRY_BUNDLE_ID_PROPERTY, uuid.toString())
          this.writer().use { props.store(it, "") }
        }
      }
    return project.layout.file(project.provider { file })
  }
}
