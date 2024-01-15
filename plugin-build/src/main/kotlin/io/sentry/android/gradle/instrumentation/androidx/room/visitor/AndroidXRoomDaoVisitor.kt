package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import io.sentry.android.gradle.instrumentation.AbstractSpanAddingMethodVisitor
import io.sentry.android.gradle.instrumentation.SpanOperations
import io.sentry.android.gradle.instrumentation.androidx.room.RoomMethodType
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AndroidXRoomDaoVisitor(
    private val className: String,
    api: Int,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?,
    private val type: RoomMethodType
) : AbstractSpanAddingMethodVisitor(
    api = api,
    originalVisitor = originalVisitor,
    access = access,
    descriptor = descriptor
) {
    private val childIfNullStatusOk = Label()
    private val childIfNullFinallyPositive = Label()
    private val childIfNullFinallyNegative = Label()
    private val startSpanIfNull = Label()
    private var finallyVisitCount = 0

    override fun visitCode() {
        super.visitCode()
        originalVisitor.visitStartSpan(startSpanIfNull) {
            visitLdcInsn(SpanOperations.DB_SQL_ROOM)
            visitLdcInsn(className)
        }
        originalVisitor.visitLabel(startSpanIfNull)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        // The backend binds the exception to the current span, via tracing without performance, so we don't need any special try/catch management.
        when {
            instrumentTransactionSuccessful(name, opcode) -> {
                // the original method wants to return, but we intervene here to set status
                originalVisitor.visitSetStatus(status = "OK", gotoIfNull = childIfNullStatusOk)
                originalVisitor.visitLabel(childIfNullStatusOk)
            }
            instrumentEndTransaction(name, opcode) || instrumentQuery(name, opcode) -> {
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

    private fun instrumentTransactionSuccessful(name: String?, opcode: Int) =
        type.isTransaction() && name == SET_TRANSACTION_SUCCESSFUL && opcode == Opcodes.INVOKEVIRTUAL

    private fun instrumentQuery(name: String?, opcode: Int) =
        type == RoomMethodType.QUERY && name == CLOSE && opcode == Opcodes.INVOKEINTERFACE

    private fun instrumentEndTransaction(name: String?, opcode: Int) =
        type.isTransaction() && name == END_TRANSACTION && opcode == Opcodes.INVOKEVIRTUAL

    companion object {
        private const val CLOSE = "close"
        private const val END_TRANSACTION = "endTransaction"
        private const val SET_TRANSACTION_SUCCESSFUL = "setTransactionSuccessful"
    }
}
