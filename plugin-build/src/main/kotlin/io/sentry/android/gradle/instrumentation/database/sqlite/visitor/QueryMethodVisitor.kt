package io.sentry.android.gradle.instrumentation.database.sqlite.visitor

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.util.concurrent.atomic.AtomicBoolean

class QueryMethodVisitor(
    private var initialVarCount: Int,
    api: Int,
    methodVisitor: MethodVisitor
) : MethodVisitor(api, methodVisitor) {

    private val label0 = Label()
    private val label1 = Label()
    private val label2 = Label()
    private val label3 = Label()
    private val label4 = Label()
    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()

    private val instrumenting = AtomicBoolean(false)

    override fun visitCode() {
        super.visitCode()
        // start visiting our query method
        visitTryCatchBlocks()

        visitStartSpan()

        // we delegate to the original method visitor to keep the original method's bytecode
        // in theory, we could rewrite the original bytecode as well, but it would mean keeping track
        // of its changes and maintaining it
        visitLabel(label0)
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode == ARETURN && !instrumenting.getAndSet(true)) {
            val cursorIndex = initialVarCount + 2
            visitVarInsn(ASTORE, cursorIndex) // Cursor cursor = ...

            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label5)

            visitStoreCursor()
            // visit finally block for the positive path in the control flow (return from try-block)
            visitFinallyBlock(label = label1, gotoIfNull = label6)
            visitReturn()

            visitCatchBlock()

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitFinallyBlock(label = label4, gotoIfNull = label8)
            val exceptionIndex = initialVarCount + 4
            visitLabel(label8)
            visitThrow(varToLoad = exceptionIndex)
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun visitStoreCursor() {
        visitLabel(label5)

        val cursorIndex = initialVarCount + 2
        val newCursorIndex = initialVarCount + 3
        visitVarInsn(ALOAD, cursorIndex)
        visitVarInsn(ASTORE, newCursorIndex)
    }

    // bytecode preparations for try-catch blocks
    private fun MethodVisitor.visitTryCatchBlocks() {
        visitTryCatchBlock(label0, label1, label2, "java/lang/Exception")
        visitTryCatchBlock(label0, label1, label3, null)
        visitTryCatchBlock(label2, label4, label3, null)
    }

    /*
    ISpan span = Sentry.getSpan()
    ISpan child = null;
    if (span != null) {
      child = span.startChild("db", supportQuery.getSql());
    }
     */
    private fun MethodVisitor.visitStartSpan() {
        visitMethodInsn(
            INVOKESTATIC,
            "io/sentry/Sentry",
            "getSpan",
            "()Lio/sentry/ISpan;",
            /* isInterface = */ false
        )
        val spanIndex = initialVarCount
        val childIndex = initialVarCount + 1
        visitVarInsn(ASTORE, spanIndex) // span
        visitInsn(ACONST_NULL)
        visitVarInsn(ASTORE, childIndex) // child
        visitVarInsn(ALOAD, spanIndex) // span
        visitJumpInsn(IFNULL, label0)
        visitVarInsn(ALOAD, spanIndex) // span
        visitLdcInsn("db")
        visitVarInsn(ALOAD, 1) // supportQuery
        visitMethodInsn(
            INVOKEINTERFACE,
            "androidx/sqlite/db/SupportSQLiteQuery",
            "getSql",
            "()Ljava/lang/String;",
            /* isInterface = */ true
        )
        visitMethodInsn(
            INVOKEINTERFACE,
            "io/sentry/ISpan",
            "startChild",
            "(Ljava/lang/String;Ljava/lang/String;)Lio/sentry/ISpan;",
            /* isInterface = */ true
        )
        visitVarInsn(ASTORE, childIndex) // child = ...
    }

    /*
    if (child != null) {
      child.setStatus(SpanStatus.OK|SpanStatus.INTERNAL_ERROR);
    }
     */
    private fun MethodVisitor.visitSetStatus(status: String, gotoIfNull: Label) {
        val childIndex = initialVarCount + 1
        visitVarInsn(ALOAD, childIndex) // child
        visitJumpInsn(IFNULL, gotoIfNull)
        visitVarInsn(ALOAD, childIndex)
        visitFieldInsn(
            GETSTATIC,
            "io/sentry/SpanStatus",
            status,
            "Lio/sentry/SpanStatus;"
        )
        visitMethodInsn(
            INVOKEINTERFACE,
            "io/sentry/ISpan",
            "setStatus",
            "(Lio/sentry/SpanStatus;)V",
            /* isInterface = */true
        )
    }

    /*
    catch (Exception e) {
      if (child != null) {
        child.setStatus(SpanStatus.INTERNAL_ERROR);
      }
      throw e;
     }
     */
    private fun MethodVisitor.visitCatchBlock() {
        val cursorIndex = initialVarCount + 2
        visitLabel(label2)
        visitVarInsn(ASTORE, cursorIndex) // Cursor cursor = ...
        visitSetStatus(status = "INTERNAL_ERROR", gotoIfNull = label7)

        visitLabel(label7)
        visitThrow(varToLoad = cursorIndex)

        val exceptionIndex = initialVarCount + 4
        visitLabel(label3)
        visitVarInsn(ASTORE, exceptionIndex) // Exception e;
    }

    /*
    finally {
       if (child != null) {
         child.finish();
       }
     }
     */
    private fun MethodVisitor.visitFinallyBlock(label: Label, gotoIfNull: Label) {
        val childIndex = initialVarCount + 1
        visitLabel(label)

        visitVarInsn(ALOAD, childIndex)
        visitJumpInsn(IFNULL, gotoIfNull)
        visitVarInsn(ALOAD, childIndex)
        visitMethodInsn(
            INVOKEINTERFACE,
            "io/sentry/ISpan",
            "finish",
            "()V",
            /* isInterface = */true
        )
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

    /*
    throw e;
     */
    private fun MethodVisitor.visitThrow(varToLoad: Int) {
        visitVarInsn(ALOAD, varToLoad) // Exception e
        visitInsn(ATHROW)
    }
}
