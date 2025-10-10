package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.BUILD_CONFIGURATION_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.DISTRIBUTION_AUTH_TOKEN_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.ORG_SLUG_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.PROJECT_SLUG_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import io.sentry.gradle.common.SentryVariant
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenerateDistributionPropertiesTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `generate distribution properties with all fields from extension`() {
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

  @Test
  fun `ext properties override extension properties`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.org.set("extension-org")
    extension.projectName.set("extension-project")
    extension.authToken.set("extension-token")

    val task: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/folder/"),
        "test",
        "debug",
        sentryOrg = "ext-org",
        sentryProject = "ext-project",
      )

    val outputDir = File(project.buildDir, "dummy/folder/")
    outputDir.mkdirs()

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    val props = PropertiesUtil.load(expectedFile)

    // ext properties should take precedence
    assertEquals("ext-org", props.getProperty(ORG_SLUG_PROPERTY))
    assertEquals("ext-project", props.getProperty(PROJECT_SLUG_PROPERTY))
    // auth token should still come from extension as it's not in ext
    assertEquals("extension-token", props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `falls back to sentry properties file when extension is not set`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension

    // Create a sentry.properties file in the project root
    val sentryPropertiesFile = File(project.projectDir, "sentry.properties")
    sentryPropertiesFile.writeText(
      """
      defaults.org=props-org
      defaults.project=props-project
      """
        .trimIndent()
    )

    // Create a mock variant that will find the properties file
    val variant = createMockVariant(project)

    val task: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/folder/"),
        "test",
        "debug",
        variant = variant,
      )

    val outputDir = File(project.buildDir, "dummy/folder/")
    outputDir.mkdirs()

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    val props = PropertiesUtil.load(expectedFile)

    assertEquals("props-org", props.getProperty(ORG_SLUG_PROPERTY))
    assertEquals("props-project", props.getProperty(PROJECT_SLUG_PROPERTY))
    // Auth token should not be present (no fallback)
    assertNull(props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `extension properties override sentry properties file`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.org.set("extension-org")
    extension.authToken.set("extension-token")

    // Create a sentry.properties file
    val sentryPropertiesFile = File(project.projectDir, "sentry.properties")
    sentryPropertiesFile.writeText(
      """
      defaults.org=props-org
      defaults.project=props-project
      """
        .trimIndent()
    )

    val variant = createMockVariant(project)

    val task: TaskProvider<GenerateDistributionPropertiesTask> =
      GenerateDistributionPropertiesTask.register(
        project,
        extension,
        null,
        project.layout.buildDirectory.dir("dummy/folder/"),
        "test",
        "debug",
        variant = variant,
      )

    val outputDir = File(project.buildDir, "dummy/folder/")
    outputDir.mkdirs()

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-distribution.properties")
    val props = PropertiesUtil.load(expectedFile)

    // Extension should override properties file
    assertEquals("extension-org", props.getProperty(ORG_SLUG_PROPERTY))
    // Project should fall back to properties file
    assertEquals("props-project", props.getProperty(PROJECT_SLUG_PROPERTY))
    // Auth token comes from extension only
    assertEquals("extension-token", props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `handles missing values gracefully`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension

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
    val props = PropertiesUtil.load(expectedFile)

    // Should not write null values
    assertNull(props.getProperty(ORG_SLUG_PROPERTY))
    assertNull(props.getProperty(PROJECT_SLUG_PROPERTY))
    assertNull(props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
    // Build configuration should always be present
    assertEquals("debug", props.getProperty(BUILD_CONFIGURATION_PROPERTY))
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }

  private fun createMockVariant(project: Project): SentryVariant {
    return object : SentryVariant {
      override val flavorName: String? = null
      override val buildTypeName: String = "debug"
      override val name: String = "debug"
      override val productFlavors: List<String> = emptyList()
      override val isDebuggable: Boolean = true
      override val isMinifyEnabled: Boolean = false

      override fun mappingFileProvider(project: Project): Provider<FileCollection> =
        project.provider { project.files() }

      override fun sources(
        project: Project,
        additionalSources: Provider<out Collection<Directory>>,
      ): Provider<out Collection<Directory>> = project.provider { emptyList<Directory>() }
    }
  }
}
