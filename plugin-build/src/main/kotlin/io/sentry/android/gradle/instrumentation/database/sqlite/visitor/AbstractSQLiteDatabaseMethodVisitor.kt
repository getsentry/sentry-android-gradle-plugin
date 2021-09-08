package io.sentry.android.gradle.instrumentation.database.sqlite.visitor

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class AbstractSQLiteDatabaseMethodVisitor(
    protected var initialVarCount: Int,
    api: Int,
    methodVisitor: MethodVisitor
) : MethodVisitor(api, methodVisitor) {

    protected val label0 = Label()
    protected val label1 = Label()
    protected val label2 = Label()
    protected val label3 = Label()
    protected val label4 = Label()

    // bytecode preparations for try-catch blocks
    protected fun MethodVisitor.visitTryCatchBlocks(expectedException: String) {
        visitTryCatchBlock(label0, label1, label2, expectedException)
        visitTryCatchBlock(label0, label1, label3, null)
        visitTryCatchBlock(label2, label4, label3, null)
    }

    /*
    ISpan span = Sentry.getSpan()
    ISpan child = null;
    if (span != null) {
      child = span.startChild("db", <description>);
    }
    */
    protected fun MethodVisitor.visitStartSpan(descriptionVisitor: MethodVisitor.() -> Unit) {
        visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "io/sentry/Sentry",
            "getSpan",
            "()Lio/sentry/ISpan;",
            /* isInterface = */ false
        )
        val spanIndex = initialVarCount
        val childIndex = initialVarCount + 1
        visitVarInsn(Opcodes.ASTORE, spanIndex) // span
        visitInsn(Opcodes.ACONST_NULL)
        visitVarInsn(Opcodes.ASTORE, childIndex) // child
        visitVarInsn(Opcodes.ALOAD, spanIndex) // span
        visitJumpInsn(Opcodes.IFNULL, label0)
        visitVarInsn(Opcodes.ALOAD, spanIndex) // span
        visitLdcInsn("db")
        descriptionVisitor()
        visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "io/sentry/ISpan",
            "startChild",
            "(Ljava/lang/String;Ljava/lang/String;)Lio/sentry/ISpan;",
            /* isInterface = */ true
        )
        visitVarInsn(Opcodes.ASTORE, childIndex) // child = ...
    }

    /*
    if (child != null) {
      child.setStatus(SpanStatus.OK|SpanStatus.INTERNAL_ERROR);
    }
     */
    protected fun MethodVisitor.visitSetStatus(status: String, gotoIfNull: Label) {
        val childIndex = initialVarCount + 1
        visitVarInsn(Opcodes.ALOAD, childIndex) // child
        visitJumpInsn(Opcodes.IFNULL, gotoIfNull)
        visitVarInsn(Opcodes.ALOAD, childIndex)
        visitFieldInsn(
            Opcodes.GETSTATIC,
            "io/sentry/SpanStatus",
            status,
            "Lio/sentry/SpanStatus;"
        )
        visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "io/sentry/ISpan",
            "setStatus",
            "(Lio/sentry/SpanStatus;)V",
            /* isInterface = */true
        )
    }

    /*
    finally {
       if (child != null) {
         child.finish();
       }
     }
     */
    protected fun MethodVisitor.visitFinallyBlock(label: Label, gotoIfNull: Label) {
        val childIndex = initialVarCount + 1
        visitLabel(label)

        visitVarInsn(Opcodes.ALOAD, childIndex)
        visitJumpInsn(Opcodes.IFNULL, gotoIfNull)
        visitVarInsn(Opcodes.ALOAD, childIndex)
        visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "io/sentry/ISpan",
            "finish",
            "()V",
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
    protected fun MethodVisitor.visitCatchBlock(
        catchLabel: Label,
        throwLabel: Label
    ) {
        val cursorIndex = initialVarCount + 2
        visitLabel(catchLabel)
        visitVarInsn(Opcodes.ASTORE, cursorIndex) // Exception e
        visitSetStatus(status = "INTERNAL_ERROR", gotoIfNull = throwLabel)

        visitLabel(throwLabel)
        visitThrow(varToLoad = cursorIndex)
    }

    /*
    throw e;
     */
    protected fun MethodVisitor.visitThrow(varToLoad: Int) {
        visitVarInsn(Opcodes.ALOAD, varToLoad) // Exception e
        visitInsn(Opcodes.ATHROW)
    }
}
