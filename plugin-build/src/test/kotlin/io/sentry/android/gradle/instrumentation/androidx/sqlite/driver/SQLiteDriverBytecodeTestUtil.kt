package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor.SetDriverMethodVisitor
import java.io.FileInputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

internal object SQLiteDriverBytecodeTestUtil {

  private const val FIXTURES_ROOT = "src/test/resources/testFixtures/instrumentation/androidxRoom"

  /**
   * Room `Builder` bytecode fixtures extracted from published AARs:
   * - `RoomDatabase$Builder.class`: `androidx.room:room-runtime-android:2.7.0`
   * - `RoomDatabase3$Builder.class`: `androidx.room3:room3-runtime-android:3.0.0-alpha06`
   *
   * Extract from Google Maven by unzipping each AAR's `classes.jar` and copying
   * `androidx/room/RoomDatabase$Builder.class` (or `androidx/room3/...`).
   *
   * `VisitorTest` needs matching Room runtime AARs (and coroutines) on the test classpath so ASM
   * can resolve types referenced by the real bytecode.
   */
  private val CLASS_NAME_TO_FIXTURE =
    mapOf(
      "androidx.room.RoomDatabase\$Builder" to "RoomDatabase\$Builder",
      "androidx.room3.RoomDatabase\$Builder" to "RoomDatabase3\$Builder",
    )

  fun loadRoomBuilderFixture(className: String): ByteArray {
    val fixtureName =
      CLASS_NAME_TO_FIXTURE[className] ?: error("No committed fixture for class $className")
    return FileInputStream("$FIXTURES_ROOT/$fixtureName.class").use { it.readBytes() }
  }

  fun isWrapCall(insn: MethodInsnNode): Boolean =
    insn.opcode == Opcodes.INVOKESTATIC &&
      insn.owner == Type.getType(SetDriverMethodVisitor.SENTRY_SQLITE_DRIVER_TYPE).internalName &&
      insn.name == SetDriverMethodVisitor.CREATE &&
      insn.desc == SetDriverMethodVisitor.SENTRY_CREATE_DESCRIPTOR

  fun isSetDriverDescriptor(descriptor: String): Boolean =
    descriptor.startsWith(SetDriverMethodInstrumentable.SET_DRIVER_DESCRIPTOR_PREFIX)

  fun countWrapCalls(bytes: ByteArray): Int {
    val classNode = ClassNode().also { ClassReader(bytes).accept(it, 0) }
    return classNode.methods.sumOf(::countWrapCalls)
  }

  fun countWrapCalls(method: MethodNode): Int =
    method.instructions.filterIsInstance<MethodInsnNode>().count(::isWrapCall)
}
