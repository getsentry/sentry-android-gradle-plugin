package io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor

import io.sentry.android.gradle.instrumentation.androidx.sqlite.AbstractAndroidXSQLiteMethodVisitor
import io.sentry.android.gradle.instrumentation.util.RETURN_CODES
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.util.concurrent.atomic.AtomicBoolean

class ExecSqlMethodVisitor(
    initialVarCount: Int,
    api: Int,
    methodVisitor: MethodVisitor
) : AbstractAndroidXSQLiteMethodVisitor(initialVarCount, api, methodVisitor) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()

    private val instrumenting = AtomicBoolean(false)

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        visitTryCatchBlocks(expectedException = "android/database/SQLException")

        visitStartSpan {
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
        if (opcode in RETURN_CODES && !instrumenting.getAndSet(true)) {
            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label1)

            // visit finally block for the positive path in the control flow (return from try-block)
            visitFinallyBlock(label = label1, gotoIfNull = label5)
            visitJumpInsn(GOTO, label5)

            visitCatchBlock(catchLabel = label2, throwLabel = label6)

            val exceptionIndex = initialVarCount + 3
            // store exception
            visitLabel(label3)
            visitVarInsn(ASTORE, exceptionIndex) // Exception e;

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitFinallyBlock(label = label4, gotoIfNull = label7)

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
