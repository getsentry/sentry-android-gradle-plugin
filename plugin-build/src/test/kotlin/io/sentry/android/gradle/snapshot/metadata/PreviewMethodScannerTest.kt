package io.sentry.android.gradle.snapshot.metadata

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class PreviewMethodScannerTest {

  @Test
  fun `finds public method annotated with Preview`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "MyPreview", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    assertEquals("MyPreview", results[0].methodName)
  }

  @Test
  fun `skips private methods when includePrivatePreviews is false`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC, "Secret", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertTrue(results.isEmpty())
  }

  @Test
  fun `includes private methods when includePrivatePreviews is true`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC, "Secret", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = true).scan(bytes)

    assertEquals(1, results.size)
    assertEquals("Secret", results[0].methodName)
  }

  @Test
  fun `ignores methods without Preview annotation`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "NotAPreview", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/runtime/Composable;", true)?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertTrue(results.isEmpty())
  }

  @Test
  fun `extracts all annotation fields`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Configured", "()V", null, null)
        val av = mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)
        av.visit("name", "Dark Theme")
        av.visit("apiLevel", 33)
        av.visit("locale", "de")
        av.visit("fontScale", 1.5f)
        av.visit("uiMode", 0x20)
        av.visit("showSystemUi", true)
        av.visit("showBackground", true)
        av.visit("backgroundColor", 0xFFFF0000L)
        av.visit("device", "spec:width=411dp,height=891dp")
        av.visit("widthDp", 411)
        av.visit("heightDp", 891)
        av.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    val config = results[0].config
    assertEquals("Dark Theme", config.name)
    assertEquals(33, config.apiLevel)
    assertEquals("de", config.locale)
    assertEquals(1.5f, config.fontScale)
    assertEquals(0x20, config.uiMode)
    assertEquals(true, config.showSystemUi)
    assertEquals(true, config.showBackground)
    assertEquals(0xFFFF0000L, config.backgroundColor)
    assertEquals("spec:width=411dp,height=891dp", config.device)
    assertEquals(411, config.widthDp)
    assertEquals(891, config.heightDp)
  }

  @Test
  fun `defaults are used when annotation has no explicit fields`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Bare", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    val config = results[0].config
    assertEquals(null, config.name)
    assertEquals(null, config.apiLevel)
    assertEquals(null, config.locale)
    assertEquals(1.0f, config.fontScale)
    assertEquals(0, config.uiMode)
    assertEquals(false, config.showSystemUi)
    assertEquals(false, config.showBackground)
    assertEquals(null, config.backgroundColor)
    assertEquals(null, config.device)
    assertEquals(null, config.widthDp)
    assertEquals(null, config.heightDp)
  }

  @Test
  fun `finds multiple preview methods in one class`() {
    val bytes =
      buildClass("com/example/ScreenKt") { cw ->
        for (name in listOf("LightPreview", "DarkPreview", "TabletPreview")) {
          val mv = cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, name, "()V", null, null)
          mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
          mv.visitEnd()
        }
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(3, results.size)
    assertEquals(
      listOf("LightPreview", "DarkPreview", "TabletPreview"),
      results.map { it.methodName },
    )
  }

  @Test
  fun `returns empty list for class with no methods`() {
    val bytes = buildClass("com/example/EmptyKt") { _ -> }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertTrue(results.isEmpty())
  }

  @Test
  fun `fullScan extracts source file name`() {
    val cw = ClassWriter(0)
    cw.visit(
      Opcodes.V11,
      Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
      "com/example/ScreenKt",
      null,
      "java/lang/Object",
      null,
    )
    cw.visitSource("Screen.kt", null)
    val mv =
      cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "ScreenPreview", "()V", null, null)
    mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
    mv.visitEnd()
    cw.visitEnd()

    val result = PreviewMethodScanner(includePrivatePreviews = false).fullScan(cw.toByteArray())

    assertEquals("Screen.kt", result.sourceFile)
    assertEquals(1, result.previewMethods.size)
    assertEquals("ScreenPreview", result.previewMethods[0].methodName)
  }

  @Test
  fun `fullScan returns null source file when not present`() {
    val bytes =
      buildClass("com/example/NoSourceKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Preview", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
        mv.visitEnd()
      }

    val result = PreviewMethodScanner(includePrivatePreviews = false).fullScan(bytes)

    assertNull(result.sourceFile)
    assertEquals(1, result.previewMethods.size)
  }

  /**
   * Builds a minimal .class file with the given internal name and allows the caller to add methods
   * via the [block] callback.
   */
  private fun buildClass(internalName: String, block: (ClassWriter) -> Unit): ByteArray {
    val cw = ClassWriter(0)
    cw.visit(
      Opcodes.V11,
      Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
      internalName,
      null,
      "java/lang/Object",
      null,
    )
    block(cw)
    cw.visitEnd()
    return cw.toByteArray()
  }
}
