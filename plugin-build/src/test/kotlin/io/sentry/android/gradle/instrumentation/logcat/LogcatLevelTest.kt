package io.sentry.android.gradle.instrumentation.logcat

import kotlin.test.assertEquals
import org.junit.Test

class LogcatLevelTest {

    @Test
    fun `test allowedLogFunctions for VERBOSE level`() {
        val allowedFunctions = LogcatLevel.VERBOSE.allowedLogFunctions()
        assertEquals(listOf("v", "d", "i", "w", "e", "wtf"), allowedFunctions)
    }

    @Test
    fun `test allowedLogFunctions for DEBUG level`() {
        val allowedFunctions = LogcatLevel.DEBUG.allowedLogFunctions()
        assertEquals(listOf("d", "i", "w", "e", "wtf"), allowedFunctions)
    }

    @Test
    fun `test allowedLogFunctions for INFO level`() {
        val allowedFunctions = LogcatLevel.INFO.allowedLogFunctions()
        assertEquals(listOf("i", "w", "e", "wtf"), allowedFunctions)
    }

    @Test
    fun `test allowedLogFunctions for WARNING level`() {
        val allowedFunctions = LogcatLevel.WARNING.allowedLogFunctions()
        assertEquals(listOf("w", "e", "wtf"), allowedFunctions)
    }

    @Test
    fun `test allowedLogFunctions for ERROR level`() {
        val allowedFunctions = LogcatLevel.ERROR.allowedLogFunctions()
        assertEquals(listOf("e", "wtf"), allowedFunctions)
    }
}
