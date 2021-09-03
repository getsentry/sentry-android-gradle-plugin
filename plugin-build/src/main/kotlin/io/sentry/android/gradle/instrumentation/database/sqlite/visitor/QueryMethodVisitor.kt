package io.sentry.android.gradle.instrumentation.database.sqlite.visitor

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.util.concurrent.atomic.AtomicBoolean

class QueryMethodVisitor(
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

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        // if instrumentation is in progress, we visit maxs (as we are responsible now for properly finishing method visit) -> otherwise we skip it
        if (instrumenting.get()) {
            super.visitMaxs(maxStack, maxLocals)
        }
    }

    override fun visitEnd() {
        // if instrumentation is in progress, we visit end (as we are responsible now for properly finishing method visit) -> otherwise we skip it
        if (instrumenting.get()) {
            super.visitEnd()
        }
    }

    override fun visitCode() {
        // start visiting our query method
        visitTryCatchBlocks()

        visitStartSpan()

        // we delegate to the original method visitor to keep the original method's bytecode
        // in theory, we could rewrite the original bytecode as well, but it would mean keeping track
        // of its changes and maintaining it
        visitLabel(label0)
        super.visitCode()
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode == ARETURN && !instrumenting.getAndSet(true)) {
            visitVarInsn(ASTORE, 4) // Cursor cursor = ...

            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label5)

            visitStoreCursor()
            // visit finally block for the positive path in the control flow (return from try-block)
            visitFinallyBlock(label = label1, gotoIfNull = label6)
            visitReturn()

            visitCatchBlock()

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitFinallyBlock(label = label4, gotoIfNull = label8)
            visitLabel(label8)
            visitThrow(varToLoad = 6)

            // finalize
            visitMaxs(5, 7)
            visitEnd()
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun visitStoreCursor() {
        visitLabel(label5)

        visitVarInsn(ALOAD, 4)
        visitVarInsn(ASTORE, 5)
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
        visitVarInsn(ASTORE, 2) // span
        visitInsn(ACONST_NULL)
        visitVarInsn(ASTORE, 3) // child
        visitVarInsn(ALOAD, 2) // span
        visitJumpInsn(IFNULL, label0)
        visitVarInsn(ALOAD, 2) // span
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
        visitVarInsn(ASTORE, 3) // child = ...
    }

    /*
    if (child != null) {
      child.setStatus(SpanStatus.OK|SpanStatus.INTERNAL_ERROR);
    }
     */
    private fun MethodVisitor.visitSetStatus(status: String, gotoIfNull: Label) {
        visitVarInsn(ALOAD, 3) // child
        visitJumpInsn(IFNULL, gotoIfNull)
        visitVarInsn(ALOAD, 3)
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
        visitLabel(label2)
        visitVarInsn(ASTORE, 4) // Cursor cursor = ...
        visitSetStatus(status = "INTERNAL_ERROR", gotoIfNull = label7)

        visitLabel(label7)
        visitThrow(varToLoad = 4)

        visitLabel(label3)
        visitVarInsn(ASTORE, 6) // Exception e;
    }

    /*
    finally {
       if (child != null) {
         child.finish();
       }
     }
     */
    private fun MethodVisitor.visitFinallyBlock(label: Label, gotoIfNull: Label) {
        visitLabel(label)

        visitVarInsn(ALOAD, 3)
        visitJumpInsn(IFNULL, gotoIfNull)
        visitVarInsn(ALOAD, 3)
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

        visitVarInsn(ALOAD, 5)
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
