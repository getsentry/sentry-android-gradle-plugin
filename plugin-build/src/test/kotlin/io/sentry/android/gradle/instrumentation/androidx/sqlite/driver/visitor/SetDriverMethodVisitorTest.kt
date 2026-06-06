@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor

import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.AndroidXSQLiteDriver
import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.SQLiteDriverBytecodeTestUtil
import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.SetDriverMethodInstrumentable
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class SetDriverMethodVisitorTest {

  @get:Rule val tmpDir = TemporaryFolder()

  @Test
  fun `wraps the driver parameter at the start of Room 2_x Builder setDriver`() {
    assertSetDriverWrappedOnce("androidx.room.RoomDatabase\$Builder")
  }

  @Test
  fun `wraps the driver parameter at the start of Room 3_x Builder setDriver`() {
    assertSetDriverWrappedOnce("androidx.room3.RoomDatabase\$Builder")
  }

  private fun assertSetDriverWrappedOnce(className: String) {
    val instrumentedBytes = instrument(className)
    val setDriverMethod = findSetDriverMethod(instrumentedBytes)

    assertEquals(
      1,
      SQLiteDriverBytecodeTestUtil.countWrapCalls(setDriverMethod),
      "setDriver should contain exactly one wrap",
    )
    assertTrue(
      wrapPrecedesOriginalBody(setDriverMethod),
      "SentrySQLiteDriver.create() must run before the original setDriver body",
    )
  }

  private fun instrument(className: String): ByteArray {
    val bytes = SQLiteDriverBytecodeTestUtil.loadRoomBuilderFixture(className)
    val classReader = ClassReader(bytes)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val classVisitor =
      AndroidXSQLiteDriver()
        .getVisitor(
          TestClassContext(className),
          Opcodes.ASM9,
          classWriter,
          parameters = TestSpanAddingParameters(inMemoryDir = tmpDir.root),
        )
    classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)
    return classWriter.toByteArray()
  }

  private fun findSetDriverMethod(bytes: ByteArray): MethodNode {
    val classNode = ClassNode().also { ClassReader(bytes).accept(it, 0) }
    return classNode.methods.first {
      it.name == SetDriverMethodInstrumentable.SET_DRIVER &&
        SQLiteDriverBytecodeTestUtil.isSetDriverDescriptor(it.desc)
    }
  }

  private fun wrapPrecedesOriginalBody(method: MethodNode): Boolean {
    val realInsns = method.instructions.toArray().filter { it.opcode >= 0 }
    val wrapIndex =
      realInsns.indexOfFirst { it is MethodInsnNode && SQLiteDriverBytecodeTestUtil.isWrapCall(it) }
    assertTrue(wrapIndex >= 0, "setDriver has no SentrySQLiteDriver.create call")
    val returnIndex =
      realInsns.indexOfFirst { it.opcode == Opcodes.ARETURN || it.opcode == Opcodes.RETURN }
    return wrapIndex < returnIndex
  }
}
