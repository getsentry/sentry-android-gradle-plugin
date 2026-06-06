@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import io.sentry.android.gradle.instrumentation.ChainedInstrumentable
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

class AndroidXSQLiteDriverTest {

  @get:Rule val tmpDir = TemporaryFolder()

  private val instrumentable = AndroidXSQLiteDriver()

  @Test
  fun `isInstrumentable returns false for sentry-owned classes`() {
    assertFalse(
      instrumentable.isInstrumentable(TestClassContext("io.sentry.sqlite.SentrySetDriver"))
    )
    assertFalse(instrumentable.isInstrumentable(TestClassContext("io.sentry.Sentry")))
  }

  @Test
  fun `isInstrumentable returns true for non-sentry classes`() {
    assertTrue(instrumentable.isInstrumentable(TestClassContext("com.example.RoomConfig")))
    assertTrue(
      instrumentable.isInstrumentable(
        TestClassContext("io.sentry.samples.instrumentation.ui.MainActivity")
      )
    )
  }

  @Test
  fun `ChainedInstrumentable skips setDriver wrapping for sentry-owned classes`() {
    val originalBytes = readFixtureBytes("SentrySetDriver")
    val instrumentedBytes =
      instrumentThroughChain("io.sentry.sqlite.SentrySetDriver", originalBytes)

    assertEquals(0, countWrapCalls(instrumentedBytes))
  }

  @Test
  fun `ChainedInstrumentable wraps setDriver for non-sentry classes`() {
    val instrumentedBytes =
      instrumentThroughChain(
        "com.example.InlineConstruction",
        readFixtureBytes("InlineConstruction"),
      )

    assertEquals(1, countWrapCalls(instrumentedBytes))
  }

  private fun instrumentThroughChain(className: String, bytes: ByteArray): ByteArray {
    val classReader = ClassReader(bytes)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val classVisitor =
      ChainedInstrumentable(listOf(instrumentable))
        .getVisitor(
          TestClassContext(className),
          Opcodes.ASM9,
          classWriter,
          parameters = TestSpanAddingParameters(inMemoryDir = tmpDir.root),
        )
    classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)
    return classWriter.toByteArray()
  }

  private fun countWrapCalls(bytes: ByteArray): Int {
    val classNode = ClassNode().also { ClassReader(bytes).accept(it, 0) }
    return classNode.methods.sumOf { method ->
      var count = 0
      var insn: AbstractInsnNode? = method.instructions.first
      while (insn != null) {
        if (insn is MethodInsnNode && isWrapCall(insn)) count++
        insn = insn.next
      }
      count
    }
  }

  private fun readFixtureBytes(fixtureName: String): ByteArray =
    FileInputStream(
        "src/test/resources/testFixtures/instrumentation/androidxSqliteDriver/$fixtureName.class"
      )
      .use { it.readBytes() }

  private fun isWrapCall(insn: MethodInsnNode): Boolean =
    insn.opcode == Opcodes.INVOKESTATIC && insn.owner == SENTRY_SQLITE_DRIVER && insn.name == CREATE

  companion object {
    private const val SENTRY_SQLITE_DRIVER = "io/sentry/sqlite/SentrySQLiteDriver"
    private const val CREATE = "create"
  }
}
