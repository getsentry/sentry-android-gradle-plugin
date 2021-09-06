package io.sentry.android.gradle.instrumentation.database.sqlite

import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.Instrumentable
import io.sentry.android.gradle.instrumentation.database.sqlite.visitor.QueryMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteDatabase : Instrumentable<ClassVisitor> {
    override val fqName: String get() = "androidx.sqlite.db.framework.FrameworkSQLiteDatabase"

    override fun getVisitor(apiVersion: Int, originalVisitor: ClassVisitor, descriptor: String?) =
        CommonClassVisitor(children, apiVersion, originalVisitor)

    override val children: List<Instrumentable<MethodVisitor>> = listOf(
        Query()
    )
}

class Query : Instrumentable<MethodVisitor> {
    override val fqName: String get() = "query"

    override fun getVisitor(apiVersion: Int, originalVisitor: MethodVisitor, descriptor: String?) =
        when (descriptor) {
            QUERY_METHOD_DESCRIPTOR -> QueryMethodVisitor(
                initialVarCount = 2,
                api = apiVersion,
                methodVisitor = originalVisitor
            )
            // The logic in another query method overload does not change, except that the number
            // of parameters changed (0:this, 1:supportQuery, 2:cancellationSignal), so we just provide
            // a different var count and base our var astore/aload on that value
            QUERY_METHOD_WITH_CANCELLATION_DESCRIPTOR -> QueryMethodVisitor(
                initialVarCount = 3,
                api = apiVersion,
                methodVisitor = originalVisitor
            )
            else -> originalVisitor
        }

    companion object {
        private const val QUERY_METHOD_DESCRIPTOR =
            "(Landroidx/sqlite/db/SupportSQLiteQuery;)Landroid/database/Cursor;"
        private const val QUERY_METHOD_WITH_CANCELLATION_DESCRIPTOR =
            "(Landroidx/sqlite/db/SupportSQLiteQuery;Landroid/os/CancellationSignal;)Landroid/database/Cursor;"
    }
}
