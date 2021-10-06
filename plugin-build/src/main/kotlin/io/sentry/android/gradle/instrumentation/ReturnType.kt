package io.sentry.android.gradle.instrumentation

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

enum class ReturnType(val loadInsn: Int, val storeInsn: Int, val returnInsn: Int) {
    INTEGER(Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IRETURN),
    FLOAT(Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.FRETURN),
    DOUBLE(Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.DRETURN),
    LONG(Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.LRETURN),
    OBJECT(Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.ARETURN),
    VOID(Opcodes.NOP, Opcodes.NOP, Opcodes.RETURN);

    companion object {
        fun returnCodes(): List<Int> = values().map { it.returnInsn }

        fun storeCodes(): List<Int> = values().map { it.storeInsn }

        fun loadCodes(): List<Int> = values().map { it.loadInsn }

        fun fromDescriptor(descriptor: String?): ReturnType {
            descriptor ?: error("Unable to convert $descriptor to ReturnType")

            val type = Type.getReturnType(descriptor).getOpcode(Opcodes.IRETURN)
            return values().find { it.returnInsn == type }
                ?: error("Unable to convert $descriptor to ReturnType")
        }
    }
}
