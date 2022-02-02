package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

class RoomQueryWithTransactionVisitor(
    className: String,
    api: Int,
    firstPassVisitor: MethodNode,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : AbstractRoomVisitor(
    className = className,
    api = api,
    originalVisitor = originalVisitor,
    access = access,
    descriptor = descriptor
) {

    private val label5 = Label()
    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()
    private val childIfNullStatusOk = Label()
    private val childIfNullFinallyPositive = Label()
    private val childIfNullFinallyNegative = Label()
    private val childIfNullCatch = Label()

    private val labelsRemapTable = mutableMapOf<Label, Label>()
    private var skipVarVisit = false
    private var finallyVisitCount = 0

    init {
        // remap labels of the all original try-catch blocks, except the last one, as we will overwrite
        // it with our newly injected catch block
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
            originalVisitor.visitStoreException(handler = label7, end = label8)
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
    }
}
