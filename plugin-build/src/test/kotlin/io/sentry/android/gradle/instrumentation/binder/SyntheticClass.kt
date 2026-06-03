package io.sentry.android.gradle.instrumentation.binder

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal object SyntheticClass {

  fun build(methodName: String, descriptor: String, body: MethodVisitor.() -> Unit): ByteArray {
    val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null)
    val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null)
    mv.visitCode()
    mv.body()
    mv.visitMaxs(0, 0)
    mv.visitEnd()
    cw.visitEnd()
    return cw.toByteArray()
  }
}
