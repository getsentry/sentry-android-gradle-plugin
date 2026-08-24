package io.sentry.android.gradle.tasks

import com.google.common.truth.Truth.assertThat
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenerateSentryBuildTimeOptionsTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `generates class availability source`() {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("project")).build()
    val outputDir = tempDir.newFolder("generated")
    val task =
      project.tasks.register("generateOptions", GenerateSentryBuildTimeOptionsTask::class.java) {
        it.moduleIds.set(setOf("com.jakewharton.timber:timber", "androidx.core:core"))
        it.output.set(outputDir)
      }

    task.get().generate()

    val source =
      outputDir.resolve("io/sentry/android/core/SentryGeneratedBuildTimeOptions.java").readText()
    assertThat(source).contains("availability.put(\"timber.log.Timber\", true);")
    assertThat(source).contains("availability.put(\"androidx.core.view.ScrollingView\", true);")
    assertThat(source).contains("availability.put(\"androidx.lifecycle.Lifecycle\", false);")
  }

  @Test
  fun `register does not resolve runtime classpath`() {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("java-project")).build()
    project.plugins.apply("java")
    val runtimeClasspath = project.configurations.getByName("runtimeClasspath")

    GenerateSentryBuildTimeOptionsTask.register(project, "runtimeClasspath", "Test")?.get()

    assertThat(runtimeClasspath.state).isEqualTo(Configuration.State.UNRESOLVED)
  }

  @Test
  fun `register includes project dependencies`() {
    val root = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("root")).build()
    val replayDir = root.file("sentry-android-replay").apply { mkdir() }
    val replay =
      ProjectBuilder.builder()
        .withName("sentry-android-replay")
        .withProjectDir(replayDir)
        .withParent(root)
        .build()
    replay.group = "io.sentry"
    replay.plugins.apply("java")
    val appDir = root.file("app").apply { mkdir() }
    val app =
      ProjectBuilder.builder().withName("app").withProjectDir(appDir).withParent(root).build()
    app.plugins.apply("java")
    app.dependencies.add(
      "implementation",
      app.dependencies.project(mapOf("path" to ":sentry-android-replay")),
    )

    val task = GenerateSentryBuildTimeOptionsTask.register(app, "runtimeClasspath", "Test")!!.get()

    assertThat(task.moduleIds.get()).contains("io.sentry:sentry-android-replay")
  }
}
