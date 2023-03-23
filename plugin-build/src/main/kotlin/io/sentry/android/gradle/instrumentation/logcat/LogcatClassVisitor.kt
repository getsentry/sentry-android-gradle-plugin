package io.sentry.android.gradle.instrumentation.logcat

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class LogcatClassVisitor(
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    private val minLevel: LogcatLevel
) :
    ClassVisitor(apiVersion, originalVisitor) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return LogMethodVisitor(mv, access, name, descriptor, minLevel)
    }
}

class LogMethodVisitor(
    mv: MethodVisitor?,
    access: Int,
    name: String?,
    desc: String?,
    private val minLevel: LogcatLevel
) :
    AdviceAdapter(Opcodes.ASM7, mv, access, name, desc) {
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        desc: String?,
        itf: Boolean
    ) {
        if (owner.contains("android/util/Log") && minLevel.allowedLogFunctions().contains(name)) {
            // Replace call to Log with call to SentryLogcatAdapter
            mv.visitMethodInsn(
                INVOKESTATIC,
                "io/sentry/android/core/SentryLogcatAdapter",
                name,
                desc,
                false
            )
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf)
        }
    }
}

