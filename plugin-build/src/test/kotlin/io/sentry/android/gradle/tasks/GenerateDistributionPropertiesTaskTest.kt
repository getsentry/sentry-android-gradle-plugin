package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.BUILD_CONFIGURATION_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.DISTRIBUTION_AUTH_TOKEN_PROPERTY
import io.sentry.android.gradle.tasks.GenerateDistributionPropertiesTask.Companion.INSTALL_GROUPS_PROPERTY
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
    extension.distribution.authToken.set("test-token")

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
    extension.distribution.authToken.set("extension-token")

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
    // auth token should still come from distribution extension
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
    // Distribution auth token should match environment variable (if set)
    val envToken = System.getenv("SENTRY_DISTRIBUTION_AUTH_TOKEN")
    assertEquals(envToken, props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `extension properties override sentry properties file`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.org.set("extension-org")
    extension.distribution.authToken.set("extension-token")

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
    // Distribution auth token comes from distribution extension
    assertEquals("extension-token", props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `distribution authToken takes precedence over main authToken`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.authToken.set("main-auth-token")
    extension.distribution.authToken.set("distribution-auth-token")

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

    // Distribution auth token should be used, not main auth token
    assertEquals("distribution-auth-token", props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
  }

  @Test
  fun `does not fall back to main authToken when distribution authToken is not set`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.authToken.set("main-auth-token")
    // Explicitly set distribution auth token to null to override the env var convention
    extension.distribution.authToken.set(null as String?)

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

    // Distribution auth token should not fall back to main auth token
    assertNull(props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
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
    // Distribution auth token should match environment variable (if set)
    val envToken = System.getenv("SENTRY_DISTRIBUTION_AUTH_TOKEN")
    assertEquals(envToken, props.getProperty(DISTRIBUTION_AUTH_TOKEN_PROPERTY))
    // Build configuration should always be present
    assertEquals("debug", props.getProperty(BUILD_CONFIGURATION_PROPERTY))
  }

  @Test
  fun `installGroups is written to properties file as comma-separated list`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.distribution.installGroups.set(setOf("internal", "beta", "alpha"))

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

    // Verify property exists and contains all groups (order may vary)
    val installGroupsValue = props.getProperty(INSTALL_GROUPS_PROPERTY)
    kotlin.test.assertNotNull(installGroupsValue)
    val groups = installGroupsValue.split(",").toSet()
    assertEquals(setOf("internal", "beta", "alpha"), groups)
  }

  @Test
  fun `empty installGroups does not write property to file`() {
    val project = createProject()
    val extension = project.extensions.findByName("sentry") as SentryPluginExtension
    extension.distribution.installGroups.set(emptySet())

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

    assertNull(props.getProperty(INSTALL_GROUPS_PROPERTY))
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

      override fun wireMappingFileToUuidTask(
        project: Project,
        task: TaskProvider<out SentryGenerateProguardUuidTask>,
        variantName: String,
        dexguardEnabled: Boolean,
      ) = Unit
    }
  }
}
