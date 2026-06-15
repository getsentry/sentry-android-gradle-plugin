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

class BinderMethodInstrumentable : MethodInstrumentable {

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor = BinderMethodVisitor(apiVersion, originalVisitor, instrumentableContext)

  override fun isInstrumentable(data: MethodContext): Boolean = true
}

private const val SENTRY_BINDER_ADAPTER =
  "io/sentry/android/core/internal/binder/SentryBinderAdapter"

class BinderMethodVisitor(
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

    // Not a tracked binder call - emit the original instruction untouched.
    if (spec == null) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      return
    }

    // The registry is keyed by owner + name only, so a lookup can match a call whose invocation
    // kind (static vs. instance) differs from the spec - e.g. an unrelated static method that
    // happens to share a name with a tracked instance method (many tracked names are generic,
    // like commit/cancel/acquire/release). That distinction decides whether a receiver sits on
    // the stack below the arguments, so instrumenting a mismatched call would spill the wrong
    // number of values and emit invalid bytecode. Skip it and emit the original instruction.
    val opcodeMatchesSpec =
      if (spec.isStatic) {
        opcode == Opcodes.INVOKESTATIC
      } else {
        opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE
      }
    if (!opcodeMatchesSpec) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      return
    }

    // We rewrite `target(args...)` into the equivalent of:
    //
    //   Object token = SentryBinderAdapter.onCallStart(component, name);
    //   try {
    //     target(args...);
    //     SentryBinderAdapter.onCallEnd(token);
    //   } catch (Throwable t) {
    //     SentryBinderAdapter.onCallEnd(token);
    //     throw t;
    //   }
    //
    // Adapter calls are emitted via `mv` (the delegate) rather than the GeneratorAdapter helpers
    // (push/invokeStatic) on purpose: those helpers route through visitMethodInsn - this very
    // override - and would recurse. `super.visitMethodInsn` is reserved for the original target
    // call so it still passes through any downstream visitors.

    // The arguments (and the receiver, for instance calls) are already on the stack. Spill them
    // into locals so we can run onCallStart first and reload them inside the try block. The stack
    // is LIFO, so arguments are popped in reverse order.
    val argTypes = Method(name, descriptor).argumentTypes
    val argLocals = IntArray(argTypes.size)
    for (i in argLocals.size - 1 downTo 0) {
      argLocals[i] = newLocal(argTypes[i])
      storeLocal(argLocals[i])
    }

    val receiverLocal =
      if (!spec.isStatic) {
        newLocal(Type.getObjectType(owner)).also { storeLocal(it) }
      } else {
        -1
      }

    // Object token = SentryBinderAdapter.onCallStart(component, name);
    mv.visitLdcInsn(spec.component)
    mv.visitLdcInsn(name)
    mv.visitMethodInsn(
      Opcodes.INVOKESTATIC,
      SENTRY_BINDER_ADAPTER,
      "onCallStart",
      "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
      false,
    )
    val tokenLocal = newLocal(Type.getObjectType("java/lang/Object"))
    storeLocal(tokenLocal)

    // SentryBinderAdapter.onCallEnd(token); - emitted on both the normal and the exceptional path.
    fun emitOnCallEnd() {
      loadLocal(tokenLocal)
      mv.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        SENTRY_BINDER_ADAPTER,
        "onCallEnd",
        "(Ljava/lang/Object;)V",
        false,
      )
    }

    val tryStart = Label()
    val tryEnd = Label()
    val catchHandler = Label()
    val afterFinally = Label()
    mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, null)

    // try { target(args...); onCallEnd(token); }
    mv.visitLabel(tryStart)
    if (!spec.isStatic) {
      loadLocal(receiverLocal)
    }
    for (local in argLocals) {
      loadLocal(local)
    }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    mv.visitLabel(tryEnd)
    emitOnCallEnd()
    mv.visitJumpInsn(Opcodes.GOTO, afterFinally)

    // catch (Throwable t) { onCallEnd(token); throw t; }
    mv.visitLabel(catchHandler)
    emitOnCallEnd()
    mv.visitInsn(Opcodes.ATHROW)

    mv.visitLabel(afterFinally)
  }
}
