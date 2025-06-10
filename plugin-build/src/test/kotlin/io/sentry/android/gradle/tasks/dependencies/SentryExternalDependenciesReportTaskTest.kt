package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory.SENTRY_DEPENDENCIES_REPORT_OUTPUT
import java.io.File
import kotlin.test.assertEquals
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryExternalDependenciesReportTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `flattens transitive dependencies into a single sorted list`() {
    val project = createRegularProject()
    val output = tempDir.newFolder("dependencies")

    val task: TaskProvider<SentryExternalDependenciesReportTaskV2> =
      project.tasks.register(
        "testDependenciesReport",
        SentryExternalDependenciesReportTaskV2::class.java,
      ) {
        it.includeReport.set(true)
          it.artifactIds.set(listOf("androidx.annotation:annotation",
              "androidx.arch.core:core-common",
              "androidx.collection:collection",
              "androidx.core:core",
              "androidx.lifecycle:lifecycle-common-java8",
              "androidx.lifecycle:lifecycle-common",
              "androidx.lifecycle:lifecycle-process",
              "androidx.lifecycle:lifecycle-runtime",
              "androidx.versionedparcelable:versionedparcelable",
              "io.sentry:sentry-android-core",
              "io.sentry:sentry"))
        it.output.set(project.layout.dir(project.provider { output }))
      }

    task.get().action()
    val outputFile = File(output, SENTRY_DEPENDENCIES_REPORT_OUTPUT)
      println(outputFile.readText())

    output.verifyContents()
  }

  @Test
  fun `skips flat jars`() {
    val project = createProjectWithFlatJars()
    val output = tempDir.newFolder("dependencies")

    val task: TaskProvider<SentryExternalDependenciesReportTaskV2> =
      project.tasks.register(
        "testDependenciesReport",
        SentryExternalDependenciesReportTaskV2::class.java,
      ) {
        it.includeReport.set(true)
        it.output.set(project.layout.dir(project.provider { output }))
      }

    task.get().action()

    val outputFile = File(output, SENTRY_DEPENDENCIES_REPORT_OUTPUT)
    assertEquals("", outputFile.readText())
  }

  @Test
  fun `skips local modules and projects`() {
    val project = createMultiModuleProject()
    val output = tempDir.newFolder("dependencies")

    val task: TaskProvider<SentryExternalDependenciesReportTaskV2> =
      project.tasks.register(
        "testDependenciesReport",
        SentryExternalDependenciesReportTaskV2::class.java,
      ) {
        it.includeReport.set(true)
        it.output.set(project.layout.dir(project.provider { output }))
      }

    task.get().action()

    val outputFile = File(output, SENTRY_DEPENDENCIES_REPORT_OUTPUT)
    assertEquals("", outputFile.readText())
  }

  private fun File.verifyContents() {
    assertEquals(
      """
            androidx.annotation:annotation
            androidx.arch.core:core-common
            androidx.collection:collection
            androidx.core:core
            androidx.lifecycle:lifecycle-common
            androidx.lifecycle:lifecycle-common-java8
            androidx.lifecycle:lifecycle-process
            androidx.lifecycle:lifecycle-runtime
            androidx.versionedparcelable:versionedparcelable
            io.sentry:sentry
            io.sentry:sentry-android-core
            """
        .trimIndent(),
      File(this, SENTRY_DEPENDENCIES_REPORT_OUTPUT).readText(),
    )
  }

  private fun createRegularProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("java")
      plugins.apply("io.sentry.android.gradle")

      repositories.mavenCentral()
      repositories.google()

      dependencies.add("implementation", "androidx.activity:activity:1.2.0")
      dependencies.add("implementation", "io.sentry:sentry-android-core:6.5.0")
      return this
    }
  }

  private fun createProjectWithFlatJars(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("java")
      plugins.apply("io.sentry.android.gradle")

      mkdir("libs")
      file("libs/local.jar").apply { createNewFile() }

      repositories.flatDir { it.dir("libs") }

      dependencies.add("implementation", ":local")
      return this
    }
  }

  private fun createMultiModuleProject(): Project {
    with(ProjectBuilder.builder().build()) {
      mkdir("module")
      val module =
        ProjectBuilder.builder()
          .withName("module")
          .withProjectDir(file("module"))
          .withParent(this)
          .build()

      mkdir("app")
      val app =
        ProjectBuilder.builder()
          .withName("app")
          .withProjectDir(file("app"))
          .withParent(this)
          .build()
      app.plugins.apply("java")
      app.plugins.apply("io.sentry.android.gradle")

      app.dependencies.add("implementation", app.dependencies.project(mapOf("path" to ":module")))

      return app
    }
  }
}
