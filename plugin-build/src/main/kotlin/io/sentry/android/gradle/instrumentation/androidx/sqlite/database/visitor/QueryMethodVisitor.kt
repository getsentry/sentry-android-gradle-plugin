package io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.ReturnType
import io.sentry.android.gradle.instrumentation.util.Types
import kotlin.properties.Delegates
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.INVOKEINTERFACE

class QueryMethodVisitor(
    api: Int,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(api, originalVisitor, access, descriptor) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()

    private var cursorIndex by Delegates.notNull<Int>()

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        originalVisitor.visitTryCatchBlocks(expectedException = "java/lang/Exception")

        originalVisitor.visitStartSpan(gotoIfNull = label0) {
            visitLdcInsn("db.sql.query")
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
        originalVisitor.visitLabel(label0)
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode in ReturnType.returnCodes() && !instrumenting.getAndSet(true)) {
            originalVisitor.visitFinalizeSpan()
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun MethodVisitor.visitFinalizeSpan() {
        cursorIndex = newLocal(Types.CURSOR)
        visitVarInsn(ASTORE, cursorIndex) // Cursor cursor = ...

        // set status to OK after the successful query
        visitSetStatus(status = "OK", gotoIfNull = label5)

        visitStoreCursor()
        // visit finally block for the positive path in the control flow (return from try-block)
        visitLabel(label1)
        visitFinallyBlock(gotoIfNull = label6)
        visitReturn()

        visitCatchBlock(catchLabel = label2, throwLabel = label7)

        val exceptionIndex = newLocal(Types.EXCEPTION)
        // store exception
        visitLabel(label3)
        visitVarInsn(ASTORE, exceptionIndex) // Exception e;

        // visit finally block for the negative path in the control flow (throw from catch-block)
        visitLabel(label4)
        visitFinallyBlock(gotoIfNull = label8)
        visitLabel(label8)
        visitThrow(varToLoad = exceptionIndex)
    }

    private fun MethodVisitor.visitStoreCursor() {
        visitLabel(label5)

        visitVarInsn(ALOAD, cursorIndex)
        cursorIndex = newLocal(Types.CURSOR)
        visitVarInsn(ASTORE, cursorIndex)
    }

    /*
    return cursor;
     */
    private fun MethodVisitor.visitReturn() {
        visitLabel(label6)

        visitVarInsn(ALOAD, cursorIndex)
        visitInsn(ARETURN)
    }
}
