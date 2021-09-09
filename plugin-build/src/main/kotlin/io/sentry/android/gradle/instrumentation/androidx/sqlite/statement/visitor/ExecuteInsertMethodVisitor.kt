package io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.visitor

import io.sentry.android.gradle.instrumentation.androidx.sqlite.AbstractSQLiteDatabaseMethodVisitor
import io.sentry.android.gradle.instrumentation.util.RETURN_CODES
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.util.concurrent.atomic.AtomicBoolean

class ExecuteInsertMethodVisitor(
    api: Int,
    methodVisitor: MethodVisitor
) : AbstractSQLiteDatabaseMethodVisitor(
    initialVarCount = 3, // this method doesn't have any params, but we introduce 2 new variables (1: description, 2: index), before creating a span
    api = api,
    methodVisitor = methodVisitor
) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()

    private val instrumenting = AtomicBoolean(false)

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        visitTryCatchBlocks("java/lang/Exception")

        // extract query description from the toString() method
        visitExtractDescription()

        visitStartSpan {
            visitVarInsn(ALOAD, 1) // description
        }

        // delegate to the original method visitor to keep the original method's bytecode
        visitLabel(label0)
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode in RETURN_CODES && !instrumenting.getAndSet(true)) {
            visitVarInsn(LSTORE, 5) // result of the original executeInsert, which is a long value

            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label5)

            visitStoreResult()
            // visit finally block for the positive path in the control flow (return from try-block)
            visitFinallyBlock(label = label1, gotoIfNull = label6)
            visitReturn()

            visitCatchBlock(catchLabel = label2, throwLabel = label7)

            val exceptionIndex = 9
            // store exception
            visitLabel(label3)
            visitVarInsn(ASTORE, exceptionIndex)

            // visit finally block for the negative path in the control flow (throw from catch-block)
            visitFinallyBlock(label = label4, gotoIfNull = label8)
            visitLabel(label8)
            visitThrow(varToLoad = exceptionIndex)
            instrumenting.set(false)
            return
        }
        super.visitInsn(opcode)
    }

    private fun MethodVisitor.visitStoreResult() {
        visitLabel(label5)

        visitVarInsn(LLOAD, 5)
        visitVarInsn(LSTORE, 7) // I have no clue, why is it 7 and not 6 here honestly..
    }

    /*
    String description = mDelegate.toString();
    int index = description.indexOf(':');
    description = description.substring(index + 2);
     */
    private fun MethodVisitor.visitExtractDescription() {
        visitVarInsn(ALOAD, 0) // this
        visitFieldInsn(
            GETFIELD,
            "androidx/sqlite/db/framework/FrameworkSQLiteStatement",
            "mDelegate",
            "Landroid/database/sqlite/SQLiteStatement;"
        )
        visitMethodInsn(
            INVOKEVIRTUAL,
            "android/database/sqlite/SQLiteStatement",
            "toString",
            "()Ljava/lang/String;",
            false
        )
        visitVarInsn(ASTORE, 1) // description
        visitVarInsn(ALOAD, 1)
        visitIntInsn(BIPUSH, 58) // ':'
        visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false)
        visitVarInsn(ISTORE, 2) // index
        visitVarInsn(ALOAD, 1) // description
        visitVarInsn(ILOAD, 2) // index
        visitInsn(ICONST_2) // 2
        visitInsn(IADD) // index + 2
        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/String",
            "substring",
            "(I)Ljava/lang/String;",
            false
        )
        visitVarInsn(ASTORE, 1) // description
    }

    /*
    return result;
    */
    private fun MethodVisitor.visitReturn() {
        visitLabel(label6)

        visitVarInsn(LLOAD, 7)
        visitInsn(LRETURN)
    }
}
