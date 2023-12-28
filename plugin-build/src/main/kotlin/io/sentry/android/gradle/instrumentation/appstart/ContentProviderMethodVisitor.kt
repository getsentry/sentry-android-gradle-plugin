package io.sentry.android.gradle.instrumentation.appstart

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.util.Types
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

/**
 * Decorates the onCreate method of a ContentProvider and adds Sentry performance monitoring by
 * calling AppStartMetrics.onContentProviderCreate and AppStartMetrics.onContentProviderPostCreate
 *
 * Due to bytecode optimization of some popular libraries (e.g. androidx.startup, MlKit)
 * we can't trust that the instrumented bytecode conforms to the Java bytecode specification.
 *
 * E.g. the following is no longer true
 * Quoting docs.oracle.com/javase/specs/jvms/se7/html/jvms-2.html#jvms-2.6.1:
 * On instance method invocation, local variable 0 is always used to pass a reference
 * to the object on which the instance method is being invoked
 * (this in the Java programming language).
 *
 */
class ContentProviderMethodVisitor(
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    instrumentableContext: MethodContext
) : AdviceAdapter(
    apiVersion,
    originalVisitor,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor
) {

    private var thisIdx = 0

    override fun onMethodEnter() {
        // add local variables, this seems to confuse the optimizer enough
        newLocal(Types.OBJECT)
        newLocal(Types.OBJECT)

        // as we can't assume that variable 0 will be this, store it away into a separate local
        thisIdx = newLocal(Types.OBJECT)

        // finally load this and store it in the local variable
        visitVarInsn(ALOAD, 0)
        visitVarInsn(ASTORE, thisIdx)

        visitVarInsn(ALOAD, thisIdx)
        box(Type.getType("Landroid/content/ContentProvider;"))

        visitMethodInsn(
            INVOKESTATIC,
            "io/sentry/android/core/performance/AppStartMetrics",
            "onContentProviderCreate",
            "(Landroid/content/ContentProvider;)V",
            false
        )
    }

    override fun onMethodExit(opcode: Int) {
        visitVarInsn(ALOAD, thisIdx)
        box(Type.getType("Landroid/content/ContentProvider;"))

        visitMethodInsn(
            INVOKESTATIC,
            "io/sentry/android/core/performance/AppStartMetrics",
            "onContentProviderPostCreate",
            "(Landroid/content/ContentProvider;)V",
            false
        )
    }
}
