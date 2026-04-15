package io.sentry.android.gradle.extensions

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SnapshotsExtensionTest {

  @Test
  fun `enabled is false by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertFalse(extension.enabled.get())
  }

  @Test
  fun `generateTests is true by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertTrue(extension.generateTests.get())
  }

  @Test
  fun `generateTests can be set to false`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    extension.generateTests.set(false)

    assertFalse(extension.generateTests.get())
  }

  @Test
  fun `includePrivatePreviews is true by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertTrue(extension.includePrivatePreviews.get())
  }

  @Test
  fun `packageTrees is empty by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertTrue(extension.packageTrees.get().isEmpty())
  }
}
