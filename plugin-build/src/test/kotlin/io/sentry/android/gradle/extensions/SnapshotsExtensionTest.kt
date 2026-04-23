package io.sentry.android.gradle.extensions

import kotlin.test.assertFalse
import kotlin.test.assertNull
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
  fun `previews generateTests is true by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertTrue(extension.previews.generateTests.get())
  }

  @Test
  fun `previews generateTests can be set to false`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    extension.previews.generateTests.set(false)

    assertFalse(extension.previews.generateTests.get())
  }

  @Test
  fun `previews includePrivatePreviews is true by default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertTrue(extension.previews.includePrivatePreviews.get())
  }

  @Test
  fun `previews theme has no default`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    assertNull(extension.previews.theme.orNull)
  }

  @Test
  fun `previews block configures sub-extension`() {
    val project = ProjectBuilder.builder().build()
    val extension = project.objects.newInstance(SnapshotsExtension::class.java)

    extension.previews { previews ->
      previews.generateTests.set(false)
      previews.includePrivatePreviews.set(false)
      previews.theme.set("AppTheme")
    }

    assertFalse(extension.previews.generateTests.get())
    assertFalse(extension.previews.includePrivatePreviews.get())
    assertTrue(extension.previews.theme.get() == "AppTheme")
  }
}
