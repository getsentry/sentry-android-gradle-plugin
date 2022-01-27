package io.sentry.android.gradle.util

import kotlin.test.assertTrue
import org.gradle.util.internal.VersionNumber
import org.junit.Test

class AgpVersionsTest {

    @Test
    fun `agp versions`() {
        assertTrue { VersionNumber.parse("7.1.0") < AgpVersions.VERSION_7_2_0_alpha06 }
        assertTrue { VersionNumber.parse("7.2.0") > AgpVersions.VERSION_7_2_0_alpha06 }
    }
}
