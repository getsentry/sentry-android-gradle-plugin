package io.sentry.android.gradle.extensions

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DistributionExtensionTest {

  @Test
  fun `enabledFor is empty by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    assertTrue(extension.enabledFor.get().isEmpty())
  }

  @Test
  fun `enabledFor can be configured with variant names`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    extension.enabledFor.set(setOf("freeDebug", "paidRelease"))

    assertEquals(setOf("freeDebug", "paidRelease"), extension.enabledFor.get())
  }
}
