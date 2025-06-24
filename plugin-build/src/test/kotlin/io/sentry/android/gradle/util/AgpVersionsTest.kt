package io.sentry.android.gradle.util

import kotlin.test.assertTrue
import org.junit.Test

class AgpVersionsTest {

  @Test
  fun `agp versions`() {
    assertTrue { SemVer.parse("7.4.0-rc01") >= AgpVersions.VERSION_7_4_0 }
  }
}
