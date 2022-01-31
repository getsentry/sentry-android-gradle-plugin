package io.sentry.android.gradle.util

import kotlin.test.assertTrue
import org.junit.Test

class AgpVersionsTest {

    @Test
    fun `agp versions`() {
        assertTrue { SemVer.parse("7.1.0") < AgpVersions.VERSION_7_2_0_alpha06 }
        assertTrue { SemVer.parse("7.2.0-alpha07") > AgpVersions.VERSION_7_2_0_alpha06 }
        assertTrue { SemVer.parse("7.2.0") > AgpVersions.VERSION_7_2_0_alpha06 }
    }
}
