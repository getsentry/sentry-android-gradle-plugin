package io.sentry.android.gradle.instrumentation.util

import org.objectweb.asm.Type

object Types {
    // COMMON
    val OBJECT = Type.getType("Ljava/lang/Object;")
    val STRING = Type.getType("Ljava/lang/String;")
    val INT = Type.INT_TYPE
    val EXCEPTION = Type.getType("Ljava/lang/Exception;")
    val ITERABLE = Type.getType("Ljava/lang/Iterable;")
    val ITERATOR = Type.getType("Ljava/util/Iterator;")
    val COLLECTION = Type.getType("Ljava/util/Collection;")

    val HUB = Type.getType("Lio/sentry/IHub;")

    // DB
    val SQL_EXCEPTION = Type.getType("Landroid/database/SQLException;")
    val CURSOR = Type.getType("Landroid/database/Cursor;")
    val SPAN = Type.getType("Lio/sentry/Span;")

    // OKHTTP
    val OKHTTP_INTERCEPTOR = Type.getType("Lokhttp3/Interceptor;")
    val SENTRY_OKHTTP_INTERCEPTOR =
        Type.getType("Lio/sentry/android/okhttp/SentryOkHttpInterceptor;")
}
