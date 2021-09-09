package io.sentry.android.gradle.instrumentation.androidx.sqlite.database

import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.Instrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor.ExecSqlMethodVisitor
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor.QueryMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteDatabase : Instrumentable<ClassVisitor> {
    override val fqName: String get() = "androidx.sqlite.db.framework.FrameworkSQLiteDatabase"

    override fun getVisitor(
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        descriptor: String?,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ) = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = fqName.substringAfterLast('.'),
        methodInstrumentables = children,
        parameters = parameters
    )

    override val children: List<Instrumentable<MethodVisitor>> = listOf(
        Query(),
        ExecSql()
    )
}

class Query : Instrumentable<MethodVisitor> {
    override val fqName: String get() = "query"

    override fun getVisitor(
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        descriptor: String?,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor =
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

class ExecSql : Instrumentable<MethodVisitor> {
    override val fqName: String get() = "execSQL"

    override fun getVisitor(
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        descriptor: String?,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor =
        when (descriptor) {
            EXECSQL_METHOD_DESCRIPTOR -> ExecSqlMethodVisitor(
                initialVarCount = 2,
                api = apiVersion,
                methodVisitor = originalVisitor
            )
            // The logic in another execSQL method overload does not change, except that the number
            // of parameters changed (0:this, 1:sql, 2:bindArgs), so we just provide
            // a different var count and base our var astore/aload on that value
            EXECSQL_WITH_BINDARGS_METHOD_DESCRIPTOR -> ExecSqlMethodVisitor(
                initialVarCount = 3,
                api = apiVersion,
                methodVisitor = originalVisitor
            )
            else -> originalVisitor
        }

    companion object {
        private const val EXECSQL_METHOD_DESCRIPTOR =
            "(Ljava/lang/String;)V"
        private const val EXECSQL_WITH_BINDARGS_METHOD_DESCRIPTOR =
            "(Ljava/lang/String;[Ljava/lang/Object;)V"
    }
}
