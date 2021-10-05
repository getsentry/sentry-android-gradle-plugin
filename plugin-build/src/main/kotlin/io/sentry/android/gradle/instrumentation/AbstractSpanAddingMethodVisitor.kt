package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.util.ReturnType
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

abstract class AbstractSpanAddingMethodVisitor(
    api: Int,
    methodVisitor: MethodVisitor,
    descriptor: String?
) : MethodVisitor(api, methodVisitor) {

    protected val instrumenting = AtomicBoolean(false)
    protected val varCount = AtomicInteger(0)
    protected var childIndex by Delegates.notNull<Int>()
    private val remapTable = mutableMapOf<Int, Int>()

    init {
        descriptor?.let {
            varCount.set(Type.getArgumentsAndReturnSizes(it) shr 2)
        }
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        var remapped: Int = `var`
        if (!instrumenting.get()) {
            if (opcode in ReturnType.storeCodes() && `var` !in remapTable) {
                remapTable[`var`] = varCount.get()
                remapped = remapTable[`var`]!!
            } else if ((opcode in ReturnType.loadCodes() || opcode in ReturnType.storeCodes()) && `var` in remapTable) {
                remapped = remapTable[`var`]!!
            }
        }
        super.visitVarInsn(opcode, remapped)
        if (opcode in ReturnType.storeCodes()) {
            val newCount =
                remapped + if (opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE) 2 else 1
            varCount.set(maxOf(newCount, varCount.get()))
        }
    }

    override fun visitFrame(
        type: Int,
        numLocal: Int,
        local: Array<out Any>?,
        numStack: Int,
        stack: Array<out Any>?
    ) {
        if (type != Opcodes.F_NEW) {
            return
        }

        var localCount = numLocal
        if (local != null) {
            for (i in 0 until numLocal) {
                if (local[i] == Opcodes.LONG || local[i] == Opcodes.DOUBLE) localCount++
            }
        }
        varCount.set(maxOf(localCount, varCount.get()))
        super.visitFrame(type, numLocal, local, numStack, stack)
    }

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
    protected fun MethodVisitor.visitStartSpan(
        gotoIfNull: Label,
        descriptionVisitor: MethodVisitor.() -> Unit
    ) {
        visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "io/sentry/Sentry",
            "getSpan",
            "()Lio/sentry/ISpan;",
            /* isInterface = */ false
        )
        val spanIndex = varCount.get()
        childIndex = varCount.get() + 1
        visitVarInsn(Opcodes.ASTORE, spanIndex) // span
        visitInsn(Opcodes.ACONST_NULL)
        visitVarInsn(Opcodes.ASTORE, childIndex) // child
        visitVarInsn(Opcodes.ALOAD, spanIndex) // span
        visitJumpInsn(Opcodes.IFNULL, gotoIfNull)
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
    protected fun MethodVisitor.visitFinallyBlock(gotoIfNull: Label) {
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
        val exceptionIndex = varCount.get()
        visitLabel(catchLabel)
        visitVarInsn(Opcodes.ASTORE, exceptionIndex) // Exception e
        visitSetStatus(status = "INTERNAL_ERROR", gotoIfNull = throwLabel)

        visitLabel(throwLabel)
        visitThrow(varToLoad = exceptionIndex)
    }

    /*
    throw e;
     */
    protected fun MethodVisitor.visitThrow(varToLoad: Int) {
        visitVarInsn(Opcodes.ALOAD, varToLoad) // Exception e
        visitInsn(Opcodes.ATHROW)
    }
}
