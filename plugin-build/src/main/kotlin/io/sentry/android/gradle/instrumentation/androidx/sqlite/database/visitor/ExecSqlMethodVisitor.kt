package io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.ReturnType
import io.sentry.android.gradle.instrumentation.util.Types
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.Opcodes.RETURN

class ExecSqlMethodVisitor(
    api: Int,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(api, originalVisitor, access, descriptor) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        originalVisitor.visitTryCatchBlocks(expectedException = "android/database/SQLException")

        originalVisitor.visitStartSpan(gotoIfNull = label0) {
            visitLdcInsn("db.sql.query")
            visitVarInsn(ALOAD, 1)
        }

        // we delegate to the original method visitor to keep the original method's bytecode
        // in theory, we could rewrite the original bytecode as well, but it would mean keeping track
        // of its changes and maintaining it
        originalVisitor.visitLabel(label0)
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode in ReturnType.returnCodes() && !instrumenting.getAndSet(true)) {
            originalVisitor.finalizeSpan()
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun MethodVisitor.finalizeSpan() {
        // set status to OK after the successful query
        visitSetStatus(status = "OK", gotoIfNull = label1)

        // visit finally block for the positive path in the control flow (return from try-block)
        visitLabel(label1)
        visitFinallyBlock(gotoIfNull = label5)
        visitJumpInsn(GOTO, label5)

        visitCatchBlock(
            catchLabel = label2,
            throwLabel = label6,
            exceptionType = Types.SQL_EXCEPTION
        )

        val exceptionIndex = newLocal(Types.SQL_EXCEPTION)
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
    }
}
