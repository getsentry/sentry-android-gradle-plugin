package io.sentry.android.gradle.instrumentation.util

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Covers ASM 9.11+ per-method compute limits. Runs via the `asmLatestTest` task; skipped under
 * `test`, which uses the older ASM that AGP brings in.
 */
class ComputeLimitsTest {

  @Before
  fun requireAsmWithComputeLimits() {
    val hasComputeLimits = ClassWriter::class.java.methods.any { it.name == "setComputeLimits" }
    assumeTrue("Requires ASM 9.11+", hasComputeLimits)
  }

  @Test
  fun `large method exceeds ASM default compute limits`() {
    val e = assertFailsWith<RuntimeException> { instrument(largeClass(), raiseLimits = false) }
    assertEquals("LimitExceededException", e::class.java.simpleName)
  }

  @Test
  fun `raiseComputeLimits lets large methods be instrumented`() {
    instrument(largeClass(), raiseLimits = true)
  }

  /** Mirrors AGP: COMPUTE_FRAMES writer + a visitor chain that touches every method. */
  private fun instrument(bytes: ByteArray, raiseLimits: Boolean): ByteArray {
    val reader = ClassReader(bytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
    val visitor =
      object : ClassVisitor(Opcodes.ASM9, writer) {
        override fun visitMethod(
          access: Int,
          name: String?,
          descriptor: String?,
          signature: String?,
          exceptions: Array<out String>?,
        ): MethodVisitor =
          object :
            MethodVisitor(
              Opcodes.ASM9,
              super.visitMethod(access, name, descriptor, signature, exceptions),
            ) {}
      }
    if (raiseLimits) visitor.findClassWriter()!!.raiseComputeLimits()
    reader.accept(visitor, 0)
    return writer.toByteArray()
  }

  /**
   * A method with many locals and many multi-edge branch targets, so frame computation exceeds
   * ASM's default 100M operations while staying under its 10MB memory limit.
   */
  private fun largeClass(locals: Int = 200, blocks: Int = 1000): ByteArray {
    val cw = ClassWriter(0)
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Large", null, "java/lang/Object", null)
    val mv = cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "large", "(I)V", null, null)
    mv.visitCode()
    for (i in 1 until locals) {
      mv.visitInsn(Opcodes.ICONST_0)
      mv.visitVarInsn(Opcodes.ISTORE, i)
    }
    repeat(blocks) {
      val next = Label()
      mv.visitVarInsn(Opcodes.ILOAD, 0)
      mv.visitTableSwitchInsn(0, 9, next, *Array(10) { next })
      mv.visitLabel(next)
    }
    mv.visitInsn(Opcodes.RETURN)
    mv.visitMaxs(1, locals)
    mv.visitEnd()
    cw.visitEnd()
    return cw.toByteArray()
  }
}
