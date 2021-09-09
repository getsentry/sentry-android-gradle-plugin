package io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor

import io.sentry.android.gradle.instrumentation.androidx.sqlite.AbstractSQLiteDatabaseMethodVisitor
import io.sentry.android.gradle.instrumentation.util.RETURN_CODES
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.util.concurrent.atomic.AtomicBoolean

class QueryMethodVisitor(
    initialVarCount: Int,
    api: Int,
    methodVisitor: MethodVisitor
) : AbstractSQLiteDatabaseMethodVisitor(initialVarCount, api, methodVisitor) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()

    private val instrumenting = AtomicBoolean(false)

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        visitTryCatchBlocks(expectedException = "java/lang/Exception")

        visitStartSpan {
            visitVarInsn(ALOAD, 1)
            visitMethodInsn(
                INVOKEINTERFACE,
                "androidx/sqlite/db/SupportSQLiteQuery",
                "getSql",
                "()Ljava/lang/String;",
                /* isInterface = */true
            )
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
            val cursorIndex = initialVarCount + 2
            visitVarInsn(ASTORE, cursorIndex) // Cursor cursor = ...

            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label5)

            visitStoreCursor()
            // visit finally block for the positive path in the control flow (return from try-block)
            visitFinallyBlock(label = label1, gotoIfNull = label6)
            visitReturn()

            visitCatchBlock(catchLabel = label2, throwLabel = label7)

            val exceptionIndex = initialVarCount + 4
            // store exception
            visitLabel(label3)
            visitVarInsn(ASTORE, exceptionIndex) // Exception e;

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitFinallyBlock(label = label4, gotoIfNull = label8)
            visitLabel(label8)
            visitThrow(varToLoad = exceptionIndex)
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun MethodVisitor.visitStoreCursor() {
        visitLabel(label5)

        val cursorIndex = initialVarCount + 2
        val newCursorIndex = initialVarCount + 3
        visitVarInsn(ALOAD, cursorIndex)
        visitVarInsn(ASTORE, newCursorIndex)
    }

    /*
    return cursor;
     */
    private fun MethodVisitor.visitReturn() {
        visitLabel(label6)

        val cursorIndex = initialVarCount + 3
        visitVarInsn(ALOAD, cursorIndex)
        visitInsn(ARETURN)
    }
}
