package io.sentry.android.gradle.instrumentation.util

import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SentryPackageNameUtilsTest {

  @Test
  fun `test is sentry class or not`() {
    assertTrue { TestClassContext("io.sentry.Sentry").isSentryClass() }
    assertFalse {
      TestClassContext("io.sentry.samples.instrumentation.ui.MainActivity").isSentryClass()
    }
    assertFalse { TestClassContext("io.sentry.samples.MainActivity").isSentryClass() }
    assertFalse { TestClassContext("io.sentry.mobile.StartActivity").isSentryClass() }
    assertFalse { TestClassContext("androidx.room.Dao").isSentryClass() }
  }
}
