package io.sentry.android.gradle.instrumentation.util

import org.objectweb.asm.Type

object Types {
    val SQL_EXCEPTION = Type.getType("Landroid/database/SQLException;")
    val CURSOR = Type.getType("Landroid/database/Cursor;")
    val SPAN = Type.getType("Lio/sentry/Span;")
    val OBJECT = Type.getType("Ljava/lang/Object;")
    val STRING = Type.getType("Ljava/lang/String;")
    val EXCEPTION = Type.getType("Ljava/lang/Exception;")
    val INT = Type.INT_TYPE
}
