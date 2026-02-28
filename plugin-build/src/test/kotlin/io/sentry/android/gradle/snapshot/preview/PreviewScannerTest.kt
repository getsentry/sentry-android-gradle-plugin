package io.sentry.android.gradle.snapshot.preview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.objectweb.asm.Opcodes

class PreviewScannerTest {

  @Test
  fun `PreviewAnnotationVisitor captures name parameter`() {
    val config = PreviewConfig()
    val visitor = PreviewAnnotationVisitor(config)
    visitor.visit("name", "Light")
    visitor.visit("showBackground", true)
    visitor.visit("widthDp", 200)
    assertEquals("Light", config.name)
    assertEquals(true, config.showBackground)
    assertEquals(200, config.widthDp)
  }

  @Test
  fun `AnnotatedMethodVisitor ignores methods without Preview`() {
    val results = mutableListOf<PreviewSnapshotConfig>()
    val visitor =
      AnnotatedMethodVisitor(Opcodes.ASM9, "someMethod", "com/example/Test", results, emptyMap())
    // No visitAnnotation calls — method has no annotations
    visitor.visitEnd()
    assertTrue(results.isEmpty())
  }

  @Test
  fun `AnnotatedMethodVisitor captures direct Preview annotation`() {
    val results = mutableListOf<PreviewSnapshotConfig>()
    val visitor =
      AnnotatedMethodVisitor(Opcodes.ASM9, "MyPreview", "com/example/TestKt", results, emptyMap())

    val annotationVisitor = visitor.visitAnnotation(PREVIEW_ANNOTATION_DESC, true)
    annotationVisitor?.visit("name", "Dark")
    annotationVisitor?.visitEnd()
    visitor.visitEnd()

    assertEquals(1, results.size)
    assertEquals("com.example.TestKt.MyPreview - Dark", results[0].displayName)
    assertEquals("com.example.TestKt", results[0].className)
    assertEquals("MyPreview", results[0].methodName)
  }

  @Test
  fun `AnnotatedMethodVisitor handles multiple Preview annotations`() {
    val results = mutableListOf<PreviewSnapshotConfig>()
    val visitor =
      AnnotatedMethodVisitor(Opcodes.ASM9, "MyPreview", "com/example/TestKt", results, emptyMap())

    // First @Preview
    val av1 = visitor.visitAnnotation(PREVIEW_ANNOTATION_DESC, true)
    av1?.visit("name", "Light")
    av1?.visitEnd()

    // Second @Preview
    val av2 = visitor.visitAnnotation(PREVIEW_ANNOTATION_DESC, true)
    av2?.visit("name", "Dark")
    av2?.visitEnd()

    visitor.visitEnd()

    assertEquals(2, results.size)
    assertEquals("com.example.TestKt.MyPreview - Light", results[0].displayName)
    assertEquals("com.example.TestKt.MyPreview - Dark", results[1].displayName)
  }

  @Test
  fun `AnnotatedMethodVisitor appends index for unnamed multiple previews`() {
    val results = mutableListOf<PreviewSnapshotConfig>()
    val visitor =
      AnnotatedMethodVisitor(Opcodes.ASM9, "MyPreview", "com/example/TestKt", results, emptyMap())

    // Two @Preview without names
    visitor.visitAnnotation(PREVIEW_ANNOTATION_DESC, true)?.visitEnd()
    visitor.visitAnnotation(PREVIEW_ANNOTATION_DESC, true)?.visitEnd()
    visitor.visitEnd()

    assertEquals(2, results.size)
    assertEquals("com.example.TestKt.MyPreview [0]", results[0].displayName)
    assertEquals("com.example.TestKt.MyPreview [1]", results[1].displayName)
  }

  @Test
  fun `SnapshotClassVisitor skips private methods when includePrivatePreviews is false`() {
    val results = mutableListOf<PreviewSnapshotConfig>()
    val visitor =
      SnapshotClassVisitor(Opcodes.ASM9, "com/example/TestKt", results, false, emptyMap())

    // Visit a private method
    val mv = visitor.visitMethod(Opcodes.ACC_PRIVATE, "privatePreview", "()V", null, null)
    // If mv is from super (not our AnnotatedMethodVisitor), the method is skipped
    mv?.visitAnnotation(PREVIEW_ANNOTATION_DESC, true)?.visitEnd()
    mv?.visitEnd()

    assertTrue(results.isEmpty())
  }

  @Test
  fun `previewConfigForAnnotation returns configs for PreviewLightDark`() {
    val configs = previewConfigForAnnotation(PREVIEW_LIGHT_DARK_ANNOTATION_DESC)
    assertEquals(2, configs?.size)
    assertEquals("light", configs?.get(0)?.name)
    assertEquals("dark", configs?.get(1)?.name)
  }

  @Test
  fun `previewConfigForAnnotation returns null for unknown annotations`() {
    val configs = previewConfigForAnnotation("Ljava/lang/Override;")
    assertEquals(null, configs)
  }
}
