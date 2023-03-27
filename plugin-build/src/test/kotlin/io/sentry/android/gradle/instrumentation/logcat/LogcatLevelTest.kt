package io.sentry.android.gradle.instrumentation.logcat

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class LogcatLevelTest {

    @Test
    fun `test logFunctionToLevel`() {
        assertEquals(LogcatLevel.VERBOSE, LogcatLevel.logFunctionToLevel("v"))
        assertEquals(LogcatLevel.DEBUG, LogcatLevel.logFunctionToLevel("d"))
        assertEquals(LogcatLevel.INFO, LogcatLevel.logFunctionToLevel("i"))
        assertEquals(LogcatLevel.WARNING, LogcatLevel.logFunctionToLevel("w"))
        assertEquals(LogcatLevel.ERROR, LogcatLevel.logFunctionToLevel("e"))
        assertEquals(LogcatLevel.ERROR, LogcatLevel.logFunctionToLevel("wtf"))
        assertNull(LogcatLevel.logFunctionToLevel("invalid"))
    }
}
