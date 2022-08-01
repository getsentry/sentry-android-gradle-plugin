package io.sentry.android.gradle.instrumentation.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class MinifiedClassDetectionTest {

    @Test
    fun `detects minified class names`() {
        val classNames = listOf(
            "l0",
            """a${'$'}a""",
            "ccc017zz",
            """ccc017zz${'$'}a""",
            "aa",
            "aa${'$'}a"
        )

        classNames.forEach {
            assertTrue(classNameLooksMinified(it, "com/example/$it"), it)
        }
    }

    @Test
    fun `does not consider non minified classes as minified`() {
        val classNames = listOf(
            "ConstantPoolHelpers",
            "FileUtil",
            """FileUtil${"$"}Inner""",
            "aa${'$'}ab",
            "Id"
        )

        classNames.forEach {
            assertFalse(classNameLooksMinified(it, "com/example/$it"), it)
        }
    }

    @Test
    fun `tests that something happens`() {
        """^\w(\\${'$'}(\w))*${'$'}""".toRegex().matches("a${'$'}a")
    }
}
