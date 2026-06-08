package io.sentry.android.gradle.instrumentation.binder

import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor

class BinderMethodVisitorTest {

  @get:Rule val tmpDir = TemporaryFolder()

  private fun instrument(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    val visitor =
      CommonClassVisitor(
        Opcodes.ASM9,
        writer,
        "TestClass",
        listOf(BinderMethodInstrumentable()),
        TestSpanAddingParameters(debugOutput = false, inMemoryDir = tmpDir.root),
      )
    reader.accept(visitor, ClassReader.SKIP_FRAMES)
    return writer.toByteArray()
  }

  private fun disassemble(bytes: ByteArray): String {
    val sw = StringWriter()
    ClassReader(bytes).accept(TraceClassVisitor(null, Textifier(), PrintWriter(sw)), 0)
    return sw.toString()
  }

  @Test
  fun `wraps known instance binder call with tracer start and end`() {
    // Method body: ContentResolver.query(uri, null, null, null, null)
    val bytes =
      SyntheticClass.build("callQuery", "(Landroid/content/ContentResolver;Landroid/net/Uri;)V") {
        visitVarInsn(Opcodes.ALOAD, 0) // resolver
        visitVarInsn(Opcodes.ALOAD, 1) // uri
        visitInsn(Opcodes.ACONST_NULL)
        visitInsn(Opcodes.ACONST_NULL)
        visitInsn(Opcodes.ACONST_NULL)
        visitInsn(Opcodes.ACONST_NULL)
        visitMethodInsn(
          Opcodes.INVOKEVIRTUAL,
          "android/content/ContentResolver",
          "query",
          "(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;",
          false,
        )
        visitInsn(Opcodes.POP)
        visitInsn(Opcodes.RETURN)
      }

    val instrumented = instrument(bytes)
    val text = disassemble(instrumented)

    assertTrue(
      text.contains("io/sentry/android/core/internal/binder/SentryBinderAdapter.onCallStart"),
      "onCallStart should be emitted:\n$text",
    )
    assertTrue(
      text.contains("io/sentry/android/core/internal/binder/SentryBinderAdapter.onCallEnd"),
      "onCallEnd should be emitted:\n$text",
    )
    assertTrue(
      text.contains("LDC \"ContentResolver\""),
      "component constant should be pushed:\n$text",
    )
    assertTrue(text.contains("LDC \"query\""), "method name constant should be pushed:\n$text")
    assertTrue(text.contains("android/content/ContentResolver.query"))
    assertTrue(text.contains("TRYCATCHBLOCK"), "try/catch handler should be emitted:\n$text")
  }

  @Test
  fun `wraps known static binder call`() {
    val bytes =
      SyntheticClass.build("callSettings", "()Ljava/lang/String;") {
        visitInsn(Opcodes.ACONST_NULL) // resolver
        visitLdcInsn("some_key")
        visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "android/provider/Settings\$Secure",
          "getString",
          "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;",
          false,
        )
        visitInsn(Opcodes.ARETURN)
      }

    val instrumented = instrument(bytes)
    val text = disassemble(instrumented)

    assertTrue(
      text.contains("io/sentry/android/core/internal/binder/SentryBinderAdapter.onCallStart")
    )
    assertTrue(text.contains("LDC \"Settings.Secure\""))
  }

  @Test
  fun `does not wrap unknown method calls`() {
    val bytes =
      SyntheticClass.build("callUnknown", "(Ljava/lang/String;)I") {
        visitVarInsn(Opcodes.ALOAD, 1)
        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
        visitInsn(Opcodes.IRETURN)
      }

    val instrumented = instrument(bytes)
    val text = disassemble(instrumented)

    assertFalse(text.contains("SentryBinderAdapter"), "unknown calls must not be wrapped:\n$text")
  }

  @Test
  fun `does not wrap when opcode does not match registry kind`() {
    // Registry marks ContentResolver.query as instance-only; emit as INVOKESTATIC and expect no
    // wrap
    val bytes =
      SyntheticClass.build("callStaticQuery", "()V") {
        visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "android/content/ContentResolver",
          "query",
          "()V",
          false,
        )
        visitInsn(Opcodes.RETURN)
      }

    val instrumented = instrument(bytes)
    val text = disassemble(instrumented)

    assertFalse(text.contains("SentryBinderAdapter"))
  }
}
