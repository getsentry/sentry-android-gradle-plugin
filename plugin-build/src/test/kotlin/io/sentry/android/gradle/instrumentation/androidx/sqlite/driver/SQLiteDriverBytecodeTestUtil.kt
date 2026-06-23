package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor.SetDriverMethodVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

internal object SQLiteDriverBytecodeTestUtil {

  /**
   * Room `Builder` bytecode loaded from published AARs on the test classpath:
   * - `androidx.room.RoomDatabase$Builder`: `androidx.room:room-runtime-android`
   * - `androidx.room3.RoomDatabase$Builder`: `androidx.room3:room3-runtime-android`
   *
   * `VisitorTest` needs matching Room runtime AARs (and coroutines) on the test classpath so ASM
   * can resolve types referenced by the real bytecode.
   */
  private val CLASS_NAME_TO_RESOURCE =
    mapOf(
      "androidx.room.RoomDatabase\$Builder" to "androidx/room/RoomDatabase\$Builder.class",
      "androidx.room3.RoomDatabase\$Builder" to "androidx/room3/RoomDatabase\$Builder.class",
    )

  fun isRoomBuilderClass(className: String): Boolean = className in CLASS_NAME_TO_RESOURCE

  fun loadRoomBuilderFixture(className: String): ByteArray {
    val resourcePath =
      CLASS_NAME_TO_RESOURCE[className] ?: error("No Room Builder fixture for class $className")
    val stream =
      SQLiteDriverBytecodeTestUtil::class.java.classLoader.getResourceAsStream(resourcePath)
        ?: error(
          "Could not load $resourcePath from test classpath. " +
            "Ensure roomRuntimeAndroid and room3RuntimeAndroid are on the test classpath."
        )
    return stream.use { it.readBytes() }
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
