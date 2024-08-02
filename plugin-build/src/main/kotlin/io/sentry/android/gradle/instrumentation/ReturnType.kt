package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.util.Types
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

enum class ReturnType(val loadInsn: Int, val storeInsn: Int, val returnInsn: Int) {
  INTEGER(Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IRETURN),
  FLOAT(Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.FRETURN),
  DOUBLE(Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.DRETURN),
  LONG(Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.LRETURN),
  OBJECT(Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.ARETURN),
  VOID(Opcodes.NOP, Opcodes.NOP, Opcodes.RETURN);

  fun toType(): Type =
    when (this) {
      INTEGER -> Type.INT_TYPE
      FLOAT -> Type.FLOAT_TYPE
      DOUBLE -> Type.DOUBLE_TYPE
      LONG -> Type.LONG_TYPE
      VOID -> Type.VOID_TYPE
      OBJECT -> Types.OBJECT
    }

  companion object {
    fun returnCodes(): List<Int> = values().map { it.returnInsn }
  }
}
