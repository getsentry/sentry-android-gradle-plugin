package io.sentry.android.gradle.tasks.dependencies

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.instrumentation.readClassAvailability
import org.gradle.api.Project
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

  private fun createProject(): Project =
    ProjectBuilder.builder().withProjectDir(tempDir.newFolder("project")).build()
}
