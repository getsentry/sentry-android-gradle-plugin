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
            QUERY_METHOD_DESCRIPTOR -> QueryMethodVisitor(apiVersion, originalVisitor)
            QUERY_METHOD_WITH_CANCELLATION_DESCRIPTOR -> QueryMethodVisitor(
                apiVersion,
                originalVisitor
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
