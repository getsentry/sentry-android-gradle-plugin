package io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.util.ReturnType
import kotlin.properties.Delegates
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.BIPUSH
import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.IADD
import org.objectweb.asm.Opcodes.ICONST_2
import org.objectweb.asm.Opcodes.ILOAD
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.Opcodes.ISTORE

class ExecuteStatementMethodVisitor(
    private val returnType: ReturnType,
    api: Int,
    methodVisitor: MethodVisitor,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(
    api = api,
    methodVisitor = methodVisitor,
    descriptor = descriptor
) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()

    private var descriptionIndex by Delegates.notNull<Int>()

    override fun visitCode() {
        super.visitCode()
        // start visiting method
        visitTryCatchBlocks("java/lang/Exception")

        // extract query description from the toString() method
        visitExtractDescription()

        visitStartSpan(gotoIfNull = label0) {
            visitVarInsn(ALOAD, descriptionIndex) // description
        }

        // delegate to the original method visitor to keep the original method's bytecode
        visitLabel(label0)
    }

    override fun visitInsn(opcode: Int) {
        // if the original method wants to return, we prevent it from doing so
        // and inject our logic
        if (opcode in ReturnType.returnCodes() && !instrumenting.getAndSet(true)) {
            val resultIndex = varCount.get()
            visitVarInsn(returnType.storeInsn, resultIndex) // result of the original query

            // set status to OK after the successful query
            visitSetStatus(status = "OK", gotoIfNull = label5)

            visitStoreResult()
            // visit finally block for the positive path in the control flow (return from try-block)
            visitLabel(label1)
            visitFinallyBlock(gotoIfNull = label6)
            visitReturn()

            visitCatchBlock(catchLabel = label2, throwLabel = label7)

            val exceptionIndex = varCount.get()
            // store exception
            visitLabel(label3)
            visitVarInsn(ASTORE, exceptionIndex)

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

    private fun MethodVisitor.visitStoreResult() {
        visitLabel(label5)

        // long and double types take 2 slots on stack
        val resultIndex = varCount.get() - if (returnType == ReturnType.LONG) 2 else 1
        val newResultIndex = varCount.get()
        visitVarInsn(returnType.loadInsn, resultIndex)
        visitVarInsn(returnType.storeInsn, newResultIndex)
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
        descriptionIndex = varCount.get()
        val indexIndex = varCount.get() + 1
        visitVarInsn(ASTORE, descriptionIndex) // description
        visitVarInsn(ALOAD, descriptionIndex)
        visitIntInsn(BIPUSH, 58) // ':'
        visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false)
        visitVarInsn(ISTORE, indexIndex) // index
        visitVarInsn(ALOAD, descriptionIndex) // description
        visitVarInsn(ILOAD, indexIndex) // index
        visitInsn(ICONST_2) // 2
        visitInsn(IADD) // index + 2
        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/String",
            "substring",
            "(I)Ljava/lang/String;",
            false
        )
        visitVarInsn(ASTORE, descriptionIndex) // description
    }

    /*
    return result;
    */
    private fun MethodVisitor.visitReturn() {
        visitLabel(label6)

        val resultIndex = varCount.get() - if (returnType == ReturnType.LONG) 2 else 1
        visitVarInsn(returnType.loadInsn, resultIndex)
        visitInsn(returnType.returnInsn)
    }
}
