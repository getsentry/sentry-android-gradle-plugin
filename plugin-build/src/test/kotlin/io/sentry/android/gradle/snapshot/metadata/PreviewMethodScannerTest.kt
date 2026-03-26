package io.sentry.android.gradle.snapshot.metadata

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        av.visit("group", "themes")
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
        av.visit("wallpaper", 2)
        av.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    val config = results[0].config
    assertEquals("Dark Theme", config.name)
    assertEquals("themes", config.group)
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
    assertEquals(2, config.wallpaper)
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
    assertEquals(null, config.group)
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
    assertEquals(null, config.wallpaper)
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

  // region @Preview.Container tests

  @Test
  fun `handles Preview Container with multiple previews on one method`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "MultiPreview",
            "()V",
            null,
            null,
          )
        // Simulate @Preview.Container { @Preview(name="Light"), @Preview(name="Dark", uiMode=32) }
        val container =
          mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview\$Container;", true)
        val array = container.visitArray("value")
        val light = array.visitAnnotation(null, "Landroidx/compose/ui/tooling/preview/Preview;")
        light.visit("name", "Light")
        light.visitEnd()
        val dark = array.visitAnnotation(null, "Landroidx/compose/ui/tooling/preview/Preview;")
        dark.visit("name", "Dark")
        dark.visit("uiMode", 32)
        dark.visitEnd()
        array.visitEnd()
        container.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(2, results.size)
    assertEquals("MultiPreview", results[0].methodName)
    assertEquals("Light", results[0].config.name)
    assertEquals("MultiPreview", results[1].methodName)
    assertEquals("Dark", results[1].config.name)
    assertEquals(32, results[1].config.uiMode)
  }

  // endregion

  // region Built-in multipreview tests

  @Test
  fun `expands PreviewLightDark into two configs`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Themed", "()V", null, null)
        mv
          .visitAnnotation("Landroidx/compose/ui/tooling/preview/PreviewLightDark;", true)
          ?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(2, results.size)
    assertEquals("Light", results[0].config.name)
    assertEquals("Dark", results[1].config.name)
    assertEquals(33, results[1].config.uiMode) // UI_MODE_NIGHT_YES | UI_MODE_TYPE_NORMAL
  }

  @Test
  fun `expands PreviewFontScale into seven configs`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Fonts", "()V", null, null)
        mv
          .visitAnnotation("Landroidx/compose/ui/tooling/preview/PreviewFontScale;", true)
          ?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(7, results.size)
    assertEquals("85%", results[0].config.name)
    assertEquals(0.85f, results[0].config.fontScale)
    assertEquals("200%", results[6].config.name)
    assertEquals(2f, results[6].config.fontScale)
  }

  @Test
  fun `expands PreviewScreenSizes into five configs`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Screens", "()V", null, null)
        mv
          .visitAnnotation("Landroidx/compose/ui/tooling/preview/PreviewScreenSizes;", true)
          ?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(5, results.size)
    assertEquals("Phone", results[0].config.name)
    assertTrue(results.all { it.config.showSystemUi })
  }

  @Test
  fun `expands PreviewDynamicColors into four configs with wallpaper`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Colors", "()V", null, null)
        mv
          .visitAnnotation("Landroidx/compose/ui/tooling/preview/PreviewDynamicColors;", true)
          ?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(4, results.size)
    assertEquals("Red", results[0].config.name)
    assertEquals(0, results[0].config.wallpaper)
    assertEquals("Yellow", results[3].config.name)
    assertEquals(3, results[3].config.wallpaper)
  }

  @Test
  fun `expands WearPreviewDevices into three configs`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "WearDevs", "()V", null, null)
        mv
          .visitAnnotation("Landroidx/wear/compose/ui/tooling/preview/WearPreviewDevices;", true)
          ?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(3, results.size)
    assertTrue(results.all { it.config.showSystemUi })
    assertTrue(results.all { it.config.showBackground })
    assertNotNull(results[0].config.group)
  }

  // endregion

  // region Custom annotation tests

  @Test
  fun `discovers and resolves custom preview annotations`() {
    val scanner = PreviewMethodScanner(includePrivatePreviews = false)

    // Build a custom annotation class annotated with @Preview
    val annotationBytes =
      buildAnnotationClass("com/example/MyCustomPreview") { cw ->
        val av = cw.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)
        av.visit("name", "Custom")
        av.visit("showBackground", true)
        av.visitEnd()
      }

    // Build a class with a method annotated with @MyCustomPreview
    val classBytes =
      buildClass("com/example/ScreenKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "MyScreen", "()V", null, null)
        mv.visitAnnotation("Lcom/example/MyCustomPreview;", true)?.visitEnd()
        mv.visitEnd()
      }

    // First pass: discover custom annotations
    val customAnnotations = mutableMapOf<String, CustomPreviewAnnotation>()
    scanner.findCustomAnnotations(annotationBytes, customAnnotations)

    // Second pass: scan with custom annotations
    val results = scanner.fullScan(classBytes, customAnnotations).previewMethods

    assertEquals(1, results.size)
    assertEquals("MyScreen", results[0].methodName)
    assertEquals("Custom", results[0].config.name)
    assertEquals(true, results[0].config.showBackground)
  }

  @Test
  fun `custom annotation with multiple previews produces multiple results`() {
    val scanner = PreviewMethodScanner(includePrivatePreviews = false)

    // Custom annotation with @Preview.Container containing two @Preview annotations
    val annotationBytes =
      buildAnnotationClass("com/example/LightDarkPreviews") { cw ->
        val container =
          cw.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview\$Container;", true)
        val array = container.visitArray("value")
        val light = array.visitAnnotation(null, "Landroidx/compose/ui/tooling/preview/Preview;")
        light.visit("name", "Light")
        light.visitEnd()
        val dark = array.visitAnnotation(null, "Landroidx/compose/ui/tooling/preview/Preview;")
        dark.visit("name", "Dark")
        dark.visit("uiMode", 32)
        dark.visitEnd()
        array.visitEnd()
        container.visitEnd()
      }

    val classBytes =
      buildClass("com/example/ScreenKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "MyScreen", "()V", null, null)
        mv.visitAnnotation("Lcom/example/LightDarkPreviews;", true)?.visitEnd()
        mv.visitEnd()
      }

    val customAnnotations = mutableMapOf<String, CustomPreviewAnnotation>()
    scanner.findCustomAnnotations(annotationBytes, customAnnotations)

    val results = scanner.fullScan(classBytes, customAnnotations).previewMethods

    assertEquals(2, results.size)
    assertEquals("Light", results[0].config.name)
    assertEquals("Dark", results[1].config.name)
    assertEquals(32, results[1].config.uiMode)
  }

  @Test
  fun `nested custom annotations inherit configs`() {
    val scanner = PreviewMethodScanner(includePrivatePreviews = false)

    // First custom annotation: @Preview(name = "Base")
    val baseAnnotationBytes =
      buildAnnotationClass("com/example/BasePreview") { cw ->
        val av = cw.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)
        av.visit("name", "Base")
        av.visitEnd()
      }

    // Second custom annotation: @BasePreview (inherits from first)
    val nestedAnnotationBytes =
      buildAnnotationClass("com/example/NestedPreview") { cw ->
        cw.visitAnnotation("Lcom/example/BasePreview;", true)?.visitEnd()
      }

    val classBytes =
      buildClass("com/example/ScreenKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "MyScreen", "()V", null, null)
        mv.visitAnnotation("Lcom/example/NestedPreview;", true)?.visitEnd()
        mv.visitEnd()
      }

    // Two iterations to resolve nested references
    val customAnnotations = mutableMapOf<String, CustomPreviewAnnotation>()
    scanner.findCustomAnnotations(baseAnnotationBytes, customAnnotations)
    scanner.findCustomAnnotations(nestedAnnotationBytes, customAnnotations)
    // Second iteration resolves nested
    scanner.findCustomAnnotations(baseAnnotationBytes, customAnnotations)
    scanner.findCustomAnnotations(nestedAnnotationBytes, customAnnotations)

    val results = scanner.fullScan(classBytes, customAnnotations).previewMethods

    assertEquals(1, results.size)
    assertEquals("Base", results[0].config.name)
  }

  @Test
  fun `custom annotation with builtin multipreview inherits configs`() {
    val scanner = PreviewMethodScanner(includePrivatePreviews = false)

    // Custom annotation that uses @PreviewLightDark
    val annotationBytes =
      buildAnnotationClass("com/example/ThemePreviews") { cw ->
        cw
          .visitAnnotation("Landroidx/compose/ui/tooling/preview/PreviewLightDark;", true)
          ?.visitEnd()
      }

    val classBytes =
      buildClass("com/example/ScreenKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "MyScreen", "()V", null, null)
        mv.visitAnnotation("Lcom/example/ThemePreviews;", true)?.visitEnd()
        mv.visitEnd()
      }

    val customAnnotations = mutableMapOf<String, CustomPreviewAnnotation>()
    scanner.findCustomAnnotations(annotationBytes, customAnnotations)

    val results = scanner.fullScan(classBytes, customAnnotations).previewMethods

    assertEquals(2, results.size)
    assertEquals("Light", results[0].config.name)
    assertEquals("Dark", results[1].config.name)
  }

  // endregion

  // region @PreviewParameter tests

  @Test
  fun `extracts PreviewParameter annotation`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "Parameterized",
            "(Ljava/lang/String;)V",
            null,
            null,
          )
        // Add parameter name via MethodParameters attribute
        mv.visitParameter("sampleData", 0)

        // Add @Preview
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()

        // Add @PreviewParameter on first parameter
        val pav =
          mv.visitParameterAnnotation(
            0,
            "Landroidx/compose/ui/tooling/preview/PreviewParameter;",
            true,
          )
        pav.visit("provider", org.objectweb.asm.Type.getType("Lcom/example/SampleDataProvider;"))
        pav.visit("limit", 5)
        pav.visitEnd()

        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    val param = results[0].previewParameter
    assertNotNull(param)
    assertEquals("sampleData", param.parameterName)
    assertEquals("com.example.SampleDataProvider", param.providerClassFqn)
    assertEquals(5, param.limit)
    assertNull(param.index)
  }

  @Test
  fun `PreviewParameter falls back to param index when name unavailable`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "Parameterized",
            "(Ljava/lang/String;)V",
            null,
            null,
          )
        // No visitParameter call — name not available

        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()

        val pav =
          mv.visitParameterAnnotation(
            0,
            "Landroidx/compose/ui/tooling/preview/PreviewParameter;",
            true,
          )
        pav.visit("provider", org.objectweb.asm.Type.getType("Lcom/example/Provider;"))
        pav.visitEnd()

        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    val param = results[0].previewParameter
    assertNotNull(param)
    assertEquals("param0", param.parameterName)
    assertEquals("com.example.Provider", param.providerClassFqn)
  }

  @Test
  fun `PreviewParameter is shared across all configs from multipreview`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "Parameterized",
            "(Ljava/lang/String;)V",
            null,
            null,
          )
        mv.visitParameter("data", 0)

        // Multiple @Preview via Container
        val container =
          mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview\$Container;", true)
        val array = container.visitArray("value")
        val p1 = array.visitAnnotation(null, "Landroidx/compose/ui/tooling/preview/Preview;")
        p1.visit("name", "A")
        p1.visitEnd()
        val p2 = array.visitAnnotation(null, "Landroidx/compose/ui/tooling/preview/Preview;")
        p2.visit("name", "B")
        p2.visitEnd()
        array.visitEnd()
        container.visitEnd()

        // @PreviewParameter
        val pav =
          mv.visitParameterAnnotation(
            0,
            "Landroidx/compose/ui/tooling/preview/PreviewParameter;",
            true,
          )
        pav.visit("provider", org.objectweb.asm.Type.getType("Lcom/example/Provider;"))
        pav.visitEnd()

        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(2, results.size)
    // Both configs should share the same preview parameter
    for (result in results) {
      assertNotNull(result.previewParameter)
      assertEquals("data", result.previewParameter!!.parameterName)
      assertEquals("com.example.Provider", result.previewParameter!!.providerClassFqn)
    }
  }

  @Test
  fun `method without PreviewParameter has null previewParameter`() {
    val bytes =
      buildClass("com/example/TestKt") { cw ->
        val mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "Simple", "()V", null, null)
        mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)?.visitEnd()
        mv.visitEnd()
      }

    val results = PreviewMethodScanner(includePrivatePreviews = false).scan(bytes)

    assertEquals(1, results.size)
    assertNull(results[0].previewParameter)
  }

  // endregion

  // region Helpers

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

  private fun buildAnnotationClass(internalName: String, block: (ClassWriter) -> Unit): ByteArray {
    val cw = ClassWriter(0)
    cw.visit(
      Opcodes.V11,
      Opcodes.ACC_PUBLIC or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT or Opcodes.ACC_ANNOTATION,
      internalName,
      null,
      "java/lang/Object",
      arrayOf("java/lang/annotation/Annotation"),
    )
    block(cw)
    cw.visitEnd()
    return cw.toByteArray()
  }

  // endregion
}
