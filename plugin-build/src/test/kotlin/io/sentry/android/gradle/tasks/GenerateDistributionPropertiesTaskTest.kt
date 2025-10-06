package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.BUILD_CONFIGURATION_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.ORG_AUTH_TOKEN_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.ORG_SLUG_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.PROJECT_SLUG_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        "debug",
        "test",
      )

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    assertTrue(expectedFile.exists())

    val props = PropertiesUtil.load(expectedFile)
    assertEquals("test-org", props.getProperty(ORG_SLUG_PROPERTY))
    assertEquals("test-project", props.getProperty(PROJECT_SLUG_PROPERTY))
    assertEquals("test-token", props.getProperty(ORG_AUTH_TOKEN_PROPERTY))
    assertEquals("debug", props.getProperty(BUILD_CONFIGURATION_PROPERTY))
  }

  @Test
  fun `generate distribution properties with only build configuration`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension

    val task: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/folder/"),
        "release",
        "test",
      )

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    assertTrue(expectedFile.exists())

    val props = PropertiesUtil.load(expectedFile)
    assertNull(props.getProperty(ORG_SLUG_PROPERTY))
    assertNull(props.getProperty(PROJECT_SLUG_PROPERTY))
    assertNull(props.getProperty(ORG_AUTH_TOKEN_PROPERTY))
    assertEquals("release", props.getProperty(BUILD_CONFIGURATION_PROPERTY))
  }

  @Test
  fun `generate distribution properties overrides on subsequent calls`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.org.set("test-org-1")
    extension.projectName.set("test-project-1")
    extension.authToken.set("test-token-1")

    val task: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/folder/"),
        "debug",
        "test",
      )

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    assertTrue(expectedFile.exists())

    val props1 = PropertiesUtil.load(expectedFile)
    assertEquals("test-org-1", props1.getProperty(ORG_SLUG_PROPERTY))
    assertEquals("test-project-1", props1.getProperty(PROJECT_SLUG_PROPERTY))
    assertEquals("test-token-1", props1.getProperty(ORG_AUTH_TOKEN_PROPERTY))

    extension.org.set("test-org-2")
    extension.projectName.set("test-project-2")
    extension.authToken.set("test-token-2")

    task.get().generateProperties()

    val props2 = PropertiesUtil.load(expectedFile)
    assertEquals("test-org-2", props2.getProperty(ORG_SLUG_PROPERTY))
    assertEquals("test-project-2", props2.getProperty(PROJECT_SLUG_PROPERTY))
    assertEquals("test-token-2", props2.getProperty(ORG_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `generate distribution properties for different build configurations`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.org.set("test-org")

    val debugTask: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/debug/"),
        "debug",
        "Debug",
      )

    val releaseTask: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/release/"),
        "release",
        "Release",
      )

    debugTask.get().generateProperties()
    releaseTask.get().generateProperties()

    val debugFile = File(project.buildDir, "dummy/debug/sentry-distribution.properties")
    val releaseFile = File(project.buildDir, "dummy/release/sentry-distribution.properties")

    assertTrue(debugFile.exists())
    assertTrue(releaseFile.exists())

    val debugProps = PropertiesUtil.load(debugFile)
    val releaseProps = PropertiesUtil.load(releaseFile)

    assertEquals("debug", debugProps.getProperty(BUILD_CONFIGURATION_PROPERTY))
    assertEquals("release", releaseProps.getProperty(BUILD_CONFIGURATION_PROPERTY))
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }
}
