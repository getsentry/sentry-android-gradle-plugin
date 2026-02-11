package io.sentry.android.gradle.extensions

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DistributionExtensionTest {

  @Test
  fun `enabled is false by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    assertFalse(extension.enabled.get())
  }

  @Test
  fun `enabled can be configured`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    extension.enabled.set(true)

    assertTrue(extension.enabled.get())
  }

  @Test
  fun `updateSdkVariants is empty by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    assertTrue(extension.updateSdkVariants.get().isEmpty())
  }

  @Test
  fun `updateSdkVariants can be configured with variant names`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    extension.updateSdkVariants.set(setOf("freeDebug", "paidRelease"))

    assertEquals(setOf("freeDebug", "paidRelease"), extension.updateSdkVariants.get())
  }

  @Test
  fun `authToken uses environment variable by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    val envToken = System.getenv("SENTRY_DISTRIBUTION_AUTH_TOKEN")
    assertEquals(envToken, extension.authToken.orNull)
  }

  @Test
  fun `authToken can be configured`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    extension.authToken.set("test-token")

    assertEquals("test-token", extension.authToken.get())
  }

  @Test
  fun `installGroups is empty by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    assertTrue(extension.installGroups.get().isEmpty())
  }

  @Test
  fun `installGroups can be configured with group names`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    extension.installGroups.set(setOf("internal", "beta"))

    assertEquals(setOf("internal", "beta"), extension.installGroups.get())
  }
}
