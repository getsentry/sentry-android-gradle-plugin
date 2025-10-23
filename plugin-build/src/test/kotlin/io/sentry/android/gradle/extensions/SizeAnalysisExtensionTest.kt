package io.sentry.android.gradle.extensions

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SizeAnalysisExtensionTest {

  @Test
  fun `enabledVariants is empty by default`() {
    val project = ProjectBuilder.builder().build()
    val extension =
      project.objects.newInstance(
        SizeAnalysisExtension::class.java,
        project.objects,
        project.providers,
      )

    assertTrue(extension.enabledVariants.get().isEmpty())
  }

  @Test
  fun `enabledVariants can be configured with variant names`() {
    val project = ProjectBuilder.builder().build()
    val extension =
      project.objects.newInstance(
        SizeAnalysisExtension::class.java,
        project.objects,
        project.providers,
      )

    extension.enabledVariants.set(setOf("release", "staging", "debug"))

    assertEquals(setOf("release", "staging", "debug"), extension.enabledVariants.get())
  }

  @Test
  fun `enabledVariants can be updated multiple times`() {
    val project = ProjectBuilder.builder().build()
    val extension =
      project.objects.newInstance(
        SizeAnalysisExtension::class.java,
        project.objects,
        project.providers,
      )

    extension.enabledVariants.set(setOf("release"))
    assertEquals(setOf("release"), extension.enabledVariants.get())

    extension.enabledVariants.set(setOf("debug", "staging"))
    assertEquals(setOf("debug", "staging"), extension.enabledVariants.get())
  }

  @Test
  fun `enabled is false by default in non-CI environment`() {
    val project = ProjectBuilder.builder().build()
    val extension =
      project.objects.newInstance(
        SizeAnalysisExtension::class.java,
        project.objects,
        project.providers,
      )

    // In test environment, CI is typically not set
    assertFalse(extension.enabled.get())
  }

  @Test
  fun `enabled can be configured`() {
    val project = ProjectBuilder.builder().build()
    val extension =
      project.objects.newInstance(
        SizeAnalysisExtension::class.java,
        project.objects,
        project.providers,
      )

    extension.enabled.set(true)

    assertTrue(extension.enabled.get())
  }

  @Test
  fun `buildConfiguration can be configured`() {
    val project = ProjectBuilder.builder().build()
    val extension =
      project.objects.newInstance(
        SizeAnalysisExtension::class.java,
        project.objects,
        project.providers,
      )

    extension.buildConfiguration.set("custom-config")

    assertEquals("custom-config", extension.buildConfiguration.get())
  }
}
