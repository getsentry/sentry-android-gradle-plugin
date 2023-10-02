package io.sentry.android.gradle.instrumentation.appstart

import io.sentry.android.gradle.instrumentation.MethodContext
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

class ApplicationMethodVisitor(
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

    override fun onMethodEnter() {
        super.onMethodEnter()

        loadThis()
        visitMethodInsn(
            INVOKESTATIC,
            "io/sentry/android/core/performance/AppStartMetrics",
            "onApplicationCreate",
            "(Landroid/app/Application;)V",
            false
        )
    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)

        loadThis()
        visitMethodInsn(
            INVOKESTATIC,
            "io/sentry/android/core/performance/AppStartMetrics",
            "onApplicationPostCreate",
            "(Landroid/app/Application;)V",
            false
        )
    }
}
