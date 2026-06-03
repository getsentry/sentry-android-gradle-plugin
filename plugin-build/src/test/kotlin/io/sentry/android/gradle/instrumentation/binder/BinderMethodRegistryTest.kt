package io.sentry.android.gradle.instrumentation.binder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class BinderMethodRegistryTest {

  @Test
  fun `lookup returns null for unknown owner`() {
    assertNull(BinderMethodRegistry.lookup("com/example/Foo", "bar"))
  }

  @Test
  fun `lookup returns null for known owner but unknown method`() {
    assertNull(BinderMethodRegistry.lookup("android/content/ContentResolver", "unknownMethod"))
  }

  @Test
  fun `lookup returns spec for known instance binder method`() {
    val spec = BinderMethodRegistry.lookup("android/content/ContentResolver", "query")
    assertNotNull(spec)
    assertEquals("ContentResolver", spec.component)
    assertFalse(spec.isStatic)
  }

  @Test
  fun `lookup returns spec for known static binder method`() {
    val spec = BinderMethodRegistry.lookup("android/provider/Settings\$Secure", "getString")
    assertNotNull(spec)
    assertEquals("Settings.Secure", spec.component)
    assertTrue(spec.isStatic)
  }

  @Test
  fun `lookup covers Context subtypes separately`() {
    assertNotNull(BinderMethodRegistry.lookup("android/content/Context", "startService"))
    assertNotNull(BinderMethodRegistry.lookup("android/content/ContextWrapper", "startService"))
  }
}
