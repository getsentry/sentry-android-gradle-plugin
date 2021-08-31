package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.database.AndroidXSQLiteClassVisitor
import org.objectweb.asm.ClassVisitor

enum class Instrumentable(val fqName: String) {
    ANDROIDX_SQLITE_DATABASE("androidx.sqlite.db.framework.FrameworkSQLiteDatabase");

    fun getClassVisitor(apiVersion: Int, classVisitor: ClassVisitor) =
        when (this) {
            ANDROIDX_SQLITE_DATABASE -> AndroidXSQLiteClassVisitor(apiVersion, classVisitor)
        }

    companion object {
        fun names(): List<String> = values().map { it.fqName }

        operator fun get(fqName: String): Instrumentable =
            values().find { it.fqName == fqName }
                ?: error("$fqName is not supported for instrumentation")
    }
}
