package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

class RoomQueryWithTransactionVisitor(
    api: Int,
    firstPassVisitor: MethodNode,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : AbstractSpanAddingMethodVisitor(
    api = api,
    originalVisitor = originalVisitor,
    access = access,
    descriptor = descriptor
) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()
    private val spanIfNull = Label()
    private val childIfNullStatusOk = Label()
    private val childIfNullFinallyPositive = Label()
    private val childIfNullFinallyNegative = Label()
    private val childIfNullCatch = Label()

    private val labelsRemapTable = mutableMapOf<Label, Label>()
    private var skipVarVisit = false
    private var finallyVisitCount = 0

    init {
        // remap labels of the all original try-catch blocks, except the last one, as we will overwrite
        // it with out newly injected catch block
        for (i in 0 until firstPassVisitor.tryCatchBlocks.size - 1) {
            val tryCatchBlock = firstPassVisitor.tryCatchBlocks[i]
            when (i) {
                0 -> {
                    labelsRemapTable[tryCatchBlock.start.label] = label0
                    labelsRemapTable[tryCatchBlock.end.label] = label1
                    labelsRemapTable[tryCatchBlock.handler.label] = label2
                }
                1 -> {
                    labelsRemapTable[tryCatchBlock.start.label] = label2
                    labelsRemapTable[tryCatchBlock.end.label] = label3
                    labelsRemapTable[tryCatchBlock.handler.label] = label2
                }
                2 -> {
                    labelsRemapTable[tryCatchBlock.start.label] = label4
                    labelsRemapTable[tryCatchBlock.end.label] = label5
                    labelsRemapTable[tryCatchBlock.handler.label] = label6
                }
            }
        }
    }

    override fun MethodVisitor.visitTryCatchBlocks(expectedException: String) {
        visitTryCatchBlock(label0, label1, label2, null)
        visitTryCatchBlock(label2, label3, label2, null)
        visitTryCatchBlock(label4, label5, label6, expectedException)
        visitTryCatchBlock(label2, label6, label6, expectedException)
        visitTryCatchBlock(label4, label5, label7, null)
        visitTryCatchBlock(label2, label8, label7, null)
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        if (!instrumenting.get()) {
            // we will rewrite try-catch blocks completely by ourselves
            return
        }
        super.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitCode() {
        super.visitCode()
        instrumenting.set(true)
        originalVisitor.visitTryCatchBlocks("java/lang/Exception")

        originalVisitor.visitStartSpan(spanIfNull) {
            visitLdcInsn(DESCRIPTION)
        }

        originalVisitor.visitLabel(spanIfNull)
        instrumenting.set(false)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        if (opcode == Opcodes.INVOKEVIRTUAL) {
            when (name) {
                SET_TRANSACTION_SUCCESSFUL -> {
                    // the original method wants to return, but we intervene here to set status
                    originalVisitor.visitSetStatus(status = "OK", gotoIfNull = childIfNullStatusOk)
                    originalVisitor.visitLabel(childIfNullStatusOk)
                }
                END_TRANSACTION -> {
                    // room's finally block ends here, we add our code to finish the span

                    // we visit finally block 2 times - one for the positive path in control flow (try) one for negative (catch)
                    // hence we need to use different labels
                    val visitCount = ++finallyVisitCount
                    val label = if (visitCount == 1) {
                        childIfNullFinallyPositive
                    } else {
                        childIfNullFinallyNegative
                    }
                    originalVisitor.visitFinallyBlock(gotoIfNull = label)
                    originalVisitor.visitLabel(label)
                }
            }
        }
    }

    override fun visitLabel(label: Label?) {
        // since we are rewriting try-catch blocks, we need to also remap the original labels with ours
        val remapped = labelsRemapTable.getOrDefault(label, label)

        if (remapped == label6 && !instrumenting.getAndSet(true)) {
            originalVisitor.visitCatchBlock(catchLabel = label6, throwLabel = childIfNullCatch)
            instrumenting.set(false)
        } else {
            super.visitLabel(remapped)
        }
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (skipVarVisit) {
            skipVarVisit = false
            return
        }
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitFrame(
        type: Int,
        numLocal: Int,
        local: Array<out Any>?,
        numStack: Int,
        stack: Array<out Any>?
    ) {
        if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
            // we only care about an outer try-catch block in case of nested blocks, hence, if the cursor
            // is in locals, it's the inner finally-block to close the cursor -> skip it
            val hasThrowableOnStack = (stack?.getOrNull(0) as? String) == "java/lang/Throwable"
            val hasCursorInLocals =
                local?.any { (it as? String) == "android/database/Cursor" } ?: false

            /**
             * We visit the labels in case there's a Throwable on stack AND no cursor in locals:
             *     - It's a query within transaction and it's a finally block
             */
            if (hasThrowableOnStack && !hasCursorInLocals) {
                originalVisitor.visitLabel(label7)
                super.visitFrame(type, numLocal, local, numStack, stack)
                val exceptionIndex = nextLocal
                originalVisitor.visitVarInsn(Opcodes.ASTORE, exceptionIndex)
                originalVisitor.visitLabel(label8)

                skipVarVisit = true
                return
            }
        }
        super.visitFrame(type, numLocal, local, numStack, stack)
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        val remappedStart = labelsRemapTable.getOrDefault(start, start)
        val remappedEnd = labelsRemapTable.getOrDefault(end, end)
        super.visitLocalVariable(name, descriptor, signature, remappedStart, remappedEnd, index)
    }

    companion object {
        private const val END_TRANSACTION = "endTransaction"
        private const val SET_TRANSACTION_SUCCESSFUL = "setTransactionSuccessful"

        private const val DESCRIPTION = "room transaction with mapping"
    }
}
