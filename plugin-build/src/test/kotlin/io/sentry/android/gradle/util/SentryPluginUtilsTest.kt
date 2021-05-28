package io.sentry.android.gradle.util

import kotlin.test.assertEquals
import org.junit.Test

class SentryPluginUtilsTest {

    @Test
    fun `capitalizes string first letter uppercase`() {
        assertEquals("Kotlin", "kotlin".capitalizeUS())
    }
}
