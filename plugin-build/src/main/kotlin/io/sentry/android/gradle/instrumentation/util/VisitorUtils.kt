package io.sentry.android.gradle.instrumentation.util

import org.objectweb.asm.Opcodes

val RETURN_CODES: Set<Int> = setOf(
    Opcodes.RETURN,
    Opcodes.IRETURN,
    Opcodes.LRETURN,
    Opcodes.ARETURN,
    Opcodes.DRETURN,
    Opcodes.FRETURN
)
