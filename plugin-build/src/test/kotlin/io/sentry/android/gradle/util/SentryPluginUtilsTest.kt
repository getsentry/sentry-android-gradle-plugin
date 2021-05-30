package io.sentry.android.gradle.util

import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import kotlin.test.assertEquals
import org.junit.Test

class SentryPluginUtilsTest {

    @Test
    fun `capitalizes string first letter uppercase`() {
        assertEquals("Kotlin", "kotlin".capitalizeUS())
    }

    @Test
    fun `capitalizes string does nothing on already capitalized string`() {
        assertEquals("Kotlin", "Kotlin".capitalizeUS())
    }

    @Test
    fun `capitalizes string returns empty on empty string`() {
        assertEquals("", "".capitalizeUS())
    }
}
