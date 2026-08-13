package io.sentry.android.gradle.tasks.dependencies

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.instrumentation.readClassAvailability
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResolveSdkClassAvailabilityTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `writes availability for present and absent modules`() {
    val project = createProject()
    val output = tempDir.newFile("availability.properties")

    val task =
      project.tasks.register(
        "testResolveSdkClassAvailability",
        ResolveSdkClassAvailabilityTask::class.java,
      ) {
        it.moduleIds.set(setOf("com.jakewharton.timber:timber", "androidx.core:core"))
        it.outputFile.set(output)
      }

    task.get().action()

    val availability = readClassAvailability(task.get().outputFile)
    assertThat(availability["timber.log.Timber"]).isTrue()
    assertThat(availability["androidx.core.view.ScrollingView"]).isTrue()
    // No compose dependency in the module set.
    assertThat(availability["androidx.compose.ui.node.Owner"]).isFalse()
    assertThat(output.readText()).contains("timber.log.Timber=true")
  }

  @Test
  fun `empty module set still writes known-absent entries`() {
    val project = createProject()
    val output = tempDir.newFile("availability-empty.properties")

    val task =
      project.tasks.register(
        "testResolveSdkClassAvailabilityEmpty",
        ResolveSdkClassAvailabilityTask::class.java,
      ) {
        it.moduleIds.set(emptySet())
        it.outputFile.set(output)
      }

    task.get().action()

    val availability = readClassAvailability(task.get().outputFile)
    assertThat(availability).isNotEmpty()
    assertThat(availability.values).doesNotContain(true)
  }

  @Test
  fun `register leaves the configuration unresolved`() {
    val project = createJavaProjectWithRuntimeClasspath()
    val configuration = project.configurations.getByName("runtimeClasspath")
    assertThat(configuration.state).isEqualTo(Configuration.State.UNRESOLVED)

    val task =
      ResolveSdkClassAvailabilityTask.register(
        project = project,
        configurationName = "runtimeClasspath",
        taskSuffix = "Test",
      )
    requireNotNull(task)
    // Realize the task object (runs the configure action) without reading moduleIds.
    task.get()

    // Same wiring used for the ASM MapProperty: depends on the output file provider without
    // reading it. Building this chain must stay lazy (no config-time resolution).
    @Suppress("UNUSED_VARIABLE")
    val availabilityProvider =
      task.flatMap { it.outputFile }.map { file -> readClassAvailability(file.asFile) }

    // Mutation oracle: wiring the task must not force config-time resolution. Evaluating
    // resolutionResult during register (instead of inside a lazy Provider) would flip this to
    // RESOLVED and fail the guard. Likewise, TaskProvider.map { task.outputFile.get() } would
    // read the file before the task runs and break the dependency edge.
    assertThat(configuration.state).isEqualTo(Configuration.State.UNRESOLVED)
  }

  private fun createProject(): Project =
    ProjectBuilder.builder().withProjectDir(tempDir.newFolder("project")).build()

  private fun createJavaProjectWithRuntimeClasspath(): Project {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("java-project")).build()
    project.plugins.apply("java")
    // No external deps needed: we only assert that register() stays lazy.
    return project
  }
}
