package io.sentry.android.gradle.instrumentation.database

import io.sentry.android.gradle.instrumentation.database.crud.QueryMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteClassVisitor(
    apiVersion: Int,
    classVisitor: ClassVisitor
) : ClassVisitor(apiVersion, classVisitor) {

    companion object {
        private const val QUERY_METHOD_NAME = "query"
        private const val QUERY_METHOD_DESCRIPTOR =
            "(Landroidx/sqlite/db/SupportSQLiteQuery;)Landroid/database/Cursor;"
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return when {
            name == QUERY_METHOD_NAME && descriptor == QUERY_METHOD_DESCRIPTOR -> // we don't instrument other method overloads
                QueryMethodVisitor(api, mv)
            else -> mv
        }
    }
}
