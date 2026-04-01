package io.sentry.android.gradle.instrumentation.binder

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

class BinderIpcMethodInstrumentable : MethodInstrumentable {

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor = BinderIpcMethodVisitor(apiVersion, originalVisitor, instrumentableContext)

  override fun isInstrumentable(data: MethodContext): Boolean = true
}

private const val SENTRY_IPC_TRACER = "io/sentry/android/core/SentryIpcTracer"

class BinderIpcMethodVisitor(
  apiVersion: Int,
  originalVisitor: MethodVisitor,
  instrumentableContext: MethodContext,
) :
  GeneratorAdapter(
    apiVersion,
    originalVisitor,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor,
  ) {

  override fun visitMethodInsn(
    opcode: Int,
    owner: String,
    name: String,
    descriptor: String,
    isInterface: Boolean,
  ) {
    val spec = BinderMethodRegistry.lookup(owner, name)
    if (spec == null) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      return
    }

    val isInstanceCall = opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE
    val isStaticCall = opcode == Opcodes.INVOKESTATIC
    if (spec.isStatic && !isStaticCall || !spec.isStatic && !isInstanceCall) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      return
    }

    val argTypes = Method(name, descriptor).argumentTypes

    // Save arguments from the stack into temp locals (reverse order for LIFO)
    val argLocals = IntArray(argTypes.size)
    for (i in argLocals.size - 1 downTo 0) {
      argLocals[i] = newLocal(argTypes[i])
      storeLocal(argLocals[i])
    }

    val receiverLocal =
      if (!spec.isStatic) {
        val local = newLocal(Type.getObjectType(owner))
        storeLocal(local)
        local
      } else {
        -1
      }

    mv.visitLdcInsn(spec.component)
    mv.visitLdcInsn(name)
    mv.visitMethodInsn(
      Opcodes.INVOKESTATIC,
      SENTRY_IPC_TRACER,
      "onCallStart",
      "(Ljava/lang/String;Ljava/lang/String;)I",
      false,
    )
    val cookieLocal = newLocal(Type.INT_TYPE)
    storeLocal(cookieLocal)

    val tryStart = Label()
    val tryEnd = Label()
    val catchHandler = Label()
    val afterFinally = Label()

    mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, null)
    mv.visitLabel(tryStart)

    if (!spec.isStatic) {
      loadLocal(receiverLocal)
    }
    for (local in argLocals) {
      loadLocal(local)
    }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

    mv.visitLabel(tryEnd)
    loadLocal(cookieLocal)
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, SENTRY_IPC_TRACER, "onCallEnd", "(I)V", false)
    mv.visitJumpInsn(Opcodes.GOTO, afterFinally)

    // catch-all handler: call onCallEnd then re-throw
    mv.visitLabel(catchHandler)
    loadLocal(cookieLocal)
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, SENTRY_IPC_TRACER, "onCallEnd", "(I)V", false)
    mv.visitInsn(Opcodes.ATHROW)

    mv.visitLabel(afterFinally)
  }
}
