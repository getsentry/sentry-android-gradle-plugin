package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.ReturnType
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class RoomCallMethodVisitor(
    private val returnType: ReturnType,
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

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()
    private val label9 = Label()

    private val remappedLabel = AtomicReference<Label>()
    private val finallyVisitCount = AtomicInteger(0)

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
        // start visiting method
        originalVisitor.visitTryCatchBlocks("java/lang/Exception")

        originalVisitor.visitStartSpan(label5) {
            visitLdcInsn(DESCRIPTION)
        }

        originalVisitor.visitLabel(label5)
        instrumenting.set(false)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        when {
            (opcode == Opcodes.INVOKEVIRTUAL && name == SET_TRANSACTION_SUCCESSFUL) -> {
                // the original method wants to return, but we intervene here to set status
                originalVisitor.visitSetStatus(status = "OK", gotoIfNull = label6)
                originalVisitor.visitLabel(label6)
                remappedLabel.set(label1)
            }
            (opcode == Opcodes.INVOKEVIRTUAL && name == END_TRANSACTION) -> {
                // room's finally block ends here, we add our code to finish the span

                // we visit finally block 2 times - one for the positive path in control flow (try) one for negative (catch)
                // hence we need to use different labels
                val visitCount = finallyVisitCount.incrementAndGet()
                val label = if (visitCount == 1) label7 else label9
                originalVisitor.visitFinallyBlock(gotoIfNull = label)
                originalVisitor.visitLabel(label)
            }
            (opcode == Opcodes.INVOKEVIRTUAL && name == BEGIN_TRANSACTION) -> {
                remappedLabel.set(label0)
            }
        }
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        if (opcode == returnType.returnInsn) {
            remappedLabel.set(label2)
        }
    }

    override fun visitLabel(label: Label?) {
        // since we are rewriting try-catch blocks, we need to also remap the original labels with ours
        val remapped = remappedLabel.getAndSet(null) ?: label

        // the original method does not have a catch block, but we add ours here
        if (remapped == label2 && !instrumenting.getAndSet(true)) {
            originalVisitor.visitCatchBlock(catchLabel = label2, throwLabel = label8)
            instrumenting.set(false)
        } else {
            super.visitLabel(remapped)
        }
    }

    override fun visitFrame(
        type: Int,
        numLocal: Int,
        local: Array<out Any>?,
        numStack: Int,
        stack: Array<out Any>?
    ) {
        if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
            val descriptor = stack?.get(0)
            if (descriptor is String && descriptor == "java/lang/Throwable") {
                originalVisitor.visitLabel(label3)
                super.visitFrame(type, numLocal, local, numStack, stack)
                originalVisitor.visitLabel(label4)
                return
            }
        }
        super.visitFrame(type, numLocal, local, numStack, stack)
    }

    companion object {
        private const val END_TRANSACTION = "endTransaction"
        private const val BEGIN_TRANSACTION = "beginTransaction"
        private const val SET_TRANSACTION_SUCCESSFUL = "setTransactionSuccessful"

        private const val DESCRIPTION = "room transaction with mapping"
    }
}
