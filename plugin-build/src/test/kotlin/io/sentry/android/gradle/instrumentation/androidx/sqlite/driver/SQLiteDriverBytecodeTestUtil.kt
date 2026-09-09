package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor.SetDriverMethodVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

internal object SQLiteDriverBytecodeTestUtil {

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
