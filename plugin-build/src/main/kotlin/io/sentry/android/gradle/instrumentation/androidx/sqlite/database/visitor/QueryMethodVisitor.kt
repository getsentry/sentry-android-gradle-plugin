package io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.util.ReturnType
import java.util.concurrent.atomic.AtomicBoolean
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*

class QueryMethodVisitor(
    api: Int,
    methodVisitor: MethodVisitor,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(api, methodVisitor, descriptor) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        visitTryCatchBlocks(expectedException = "java/lang/Exception")

        visitStartSpan(gotoIfNull = label0) {
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
        if (opcode in ReturnType.returnCodes() && !instrumenting.getAndSet(true)) {
            val cursorIndex = varCount.get()
            visitVarInsn(ASTORE, cursorIndex) // Cursor cursor = ...

            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label5)

            visitStoreCursor()
            // visit finally block for the positive path in the control flow (return from try-block)
            visitLabel(label1)
            visitFinallyBlock(gotoIfNull = label6)
            visitReturn()

            visitCatchBlock(catchLabel = label2, throwLabel = label7)

            val exceptionIndex = varCount.get()
            // store exception
            visitLabel(label3)
            visitVarInsn(ASTORE, exceptionIndex) // Exception e;

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitLabel(label4)
            visitFinallyBlock(gotoIfNull = label8)
            visitLabel(label8)
            visitThrow(varToLoad = exceptionIndex)
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun MethodVisitor.visitStoreCursor() {
        visitLabel(label5)

        val cursorIndex = varCount.get() - 1
        val newCursorIndex = varCount.get()
        visitVarInsn(ALOAD, cursorIndex)
        visitVarInsn(ASTORE, newCursorIndex)
    }

    /*
    return cursor;
     */
    private fun MethodVisitor.visitReturn() {
        visitLabel(label6)

        val cursorIndex = varCount.get() - 1
        visitVarInsn(ALOAD, cursorIndex)
        visitInsn(ARETURN)
    }
}
