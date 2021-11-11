package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

abstract class AbstractRoomVisitor(
    private val className: String,
    api: Int,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(
    api = api,
    originalVisitor = originalVisitor,
    access = access,
    descriptor = descriptor
) {

    private val startSpanIfNull = Label()

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        if (!instrumenting.get()) {
            // we will rewrite try-catch blocks completely by ourselves
            return
        }
        super.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitCode() {
        super.visitCode()
        instrumenting.set(true)
        originalVisitor.visitTryCatchBlocks("java/lang/Exception")

        originalVisitor.visitStartSpan(startSpanIfNull) {
            visitLdcInsn(className)
        }

        originalVisitor.visitLabel(startSpanIfNull)
        instrumenting.set(false)
    }
}
