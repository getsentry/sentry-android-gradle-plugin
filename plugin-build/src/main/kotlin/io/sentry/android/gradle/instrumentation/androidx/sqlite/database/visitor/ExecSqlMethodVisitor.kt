package io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.util.ReturnType
import java.util.concurrent.atomic.AtomicBoolean
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*

class ExecSqlMethodVisitor(
    api: Int,
    methodVisitor: MethodVisitor,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(api, methodVisitor, descriptor) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        visitTryCatchBlocks(expectedException = "android/database/SQLException")

        visitStartSpan(gotoIfNull = label0) {
            visitVarInsn(ALOAD, 1)
        }

        // we delegate to the original method visitor to keep the original method's bytecode
        // in theory, we could rewrite the original bytecode as well, but it would mean keeping track
        // of its changes and maintaining it
        visitLabel(label0)
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode in ReturnType.returnCodes() && !instrumenting.getAndSet(true)) {
            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label1)

            // visit finally block for the positive path in the control flow (return from try-block)
            visitLabel(label1)
            visitFinallyBlock(gotoIfNull = label5)
            visitJumpInsn(GOTO, label5)

            visitCatchBlock(catchLabel = label2, throwLabel = label6)

            val exceptionIndex = varCount.get()
            // store exception
            visitLabel(label3)
            visitVarInsn(ASTORE, exceptionIndex) // Exception e;

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitLabel(label4)
            visitFinallyBlock(gotoIfNull = label7)

            visitLabel(label7)
            visitThrow(varToLoad = exceptionIndex)

            visitLabel(label5)
            visitInsn(RETURN)

            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }
}
