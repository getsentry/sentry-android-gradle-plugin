package io.sentry.android.gradle.instrumentation.util

import org.objectweb.asm.Opcodes

enum class ReturnType(val loadInsn: Int, val storeInsn: Int, val returnInsn: Int) {
    INTEGER(Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IRETURN),
    FLOAT(Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.FRETURN),
    DOUBLE(Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.DRETURN),
    LONG(Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.LRETURN),
    OBJECT(Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.ARETURN),
    VOID(Opcodes.NOP, Opcodes.NOP, Opcodes.RETURN);

    companion object {
        fun returnCodes(): List<Int> = values().map { it.returnInsn }
    }
}
