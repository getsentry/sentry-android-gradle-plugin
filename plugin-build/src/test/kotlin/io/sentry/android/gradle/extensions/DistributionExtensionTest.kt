package io.sentry.android.gradle.extensions

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DistributionExtensionTest {

  @Test
  fun `enabledVariants is empty by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    assertTrue(extension.enabledVariants.get().isEmpty())
  }

  @Test
  fun `enabledVariants can be configured with variant names`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(DistributionExtension::class.java)

    extension.enabledVariants.set(setOf("freeDebug", "paidRelease"))

    assertEquals(setOf("freeDebug", "paidRelease"), extension.enabledVariants.get())
  }
}
