package io.sentry.android.gradle.instrumentation.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class MinifiedClassDetectionTest {

  @Test
  fun `detects minified class names`() {
    val classNames =
      listOf(
        "l0",
        """a${'$'}a""",
        "ccc017zz",
        """ccc017zz${'$'}a""",
        "aa",
        "aa${'$'}a",
        "ab",
        "aa${'$'}ab",
        "ab${'$'}a",
      )

    classNames.forEach { assertTrue(classNameLooksMinified(it, "com/example/$it"), it) }
  }

  @Test
  fun `detects minified class names with minified package name`() {
    val classNames = listOf("""a${'$'}""", "aa")

    classNames.forEach { assertTrue(classNameLooksMinified(it, "a/$it"), it) }
  }

  @Test
  fun `does not consider non minified classes as minified`() {
    val classNames = listOf("ConstantPoolHelpers", "FileUtil", """FileUtil${"$"}Inner""")

    classNames.forEach { assertFalse(classNameLooksMinified(it, "com/example/$it"), it) }
  }

  @Test
  fun `does not consider short class names as minified classes`() {
    val classNames =
      listOf(
        Pair("Call", "retrofit2/Call"),
        Pair("Call", "okhttp3/Call"),
        Pair("Fill", "androidx/compose/ui/graphics/drawscope/Fill"),
        Pair("Px", "androidx/annotation/Px"),
        Pair("Dp", "androidx/annotation/Dp"),
      )

    classNames.forEach { (simpleName, fullName) ->
      assertFalse(classNameLooksMinified(simpleName, fullName))
    }
  }
}
