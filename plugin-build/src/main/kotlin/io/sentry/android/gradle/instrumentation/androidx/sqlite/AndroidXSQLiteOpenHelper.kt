package io.sentry.android.gradle.instrumentation.androidx.sqlite

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.visitor.SQLiteOpenHelperMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteOpenHelper : ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        val currentClassName = instrumentableContext.currentClassData.className
        val sqLiteMethodList: List<MethodInstrumentable> = listOf(
            SQLiteOpenHelperMethodInstrumentable()
        )
        return CommonClassVisitor(
            apiVersion,
            originalVisitor,
            currentClassName.substringAfterLast('.'),
            sqLiteMethodList,
            parameters
        )
    }

    // We want to instrument any class implementing the androidx.sqlite.db.SupportSQLiteOpenHelper$Factory
    override fun isInstrumentable(data: ClassContext) =
        data.currentClassData.interfaces.contains("androidx.sqlite.db.SupportSQLiteOpenHelper\$Factory")
}

class SQLiteOpenHelperMethodInstrumentable : MethodInstrumentable {

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor {
        return SQLiteOpenHelperMethodVisitor(
            apiVersion,
            originalVisitor,
            instrumentableContext
        )
    }

    // We want to instrument only the SupportSQLiteOpenHelper.Factory method
    //  fun create(delegate: SupportSQLiteOpenHelper): SupportSQLiteOpenHelper {...}
    override fun isInstrumentable(data: MethodContext) =
        data.descriptor == "(Landroidx/sqlite/db/SupportSQLiteOpenHelper\$Configuration;)Landroidx/sqlite/db/SupportSQLiteOpenHelper;" &&
            data.name == "create"
}
