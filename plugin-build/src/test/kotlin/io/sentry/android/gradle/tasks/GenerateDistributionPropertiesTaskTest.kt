package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.BUILD_CONFIGURATION_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.DISTRIBUTION_AUTH_TOKEN_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.ORG_SLUG_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.PROJECT_SLUG_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class GenerateDistributionPropertiesTaskTest {

  @Test
  fun `generate distribution properties with all fields`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.org.set("test-org")
    extension.projectName.set("test-project")
    extension.authToken.set("test-token")

    val task: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/folder/"),
        "test",
        "debug",
      )

    val outputDir = File(project.buildDir, "dummy/folder/")
    outputDir.mkdirs()

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    assertTrue(expectedFile.exists())

    val props = PropertiesUtil.load(expectedFile)
    assertEquals("test-org", props.getProperty(ORG_SLUG_PROPERTY))
    assertEquals("test-project", props.getProperty(PROJECT_SLUG_PROPERTY))
    assertEquals("test-token", props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
    assertEquals("debug", props.getProperty(BUILD_CONFIGURATION_PROPERTY))
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }
}
