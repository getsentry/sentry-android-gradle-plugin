@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.androidx.sqlite.statement

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.ReturnType
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.visitor.ExecuteStatementMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteStatement : ClassInstrumentable {

    override val fqName: String get() = "androidx.sqlite.db.framework.FrameworkSQLiteStatement"

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = fqName.substringAfterLast('.'),
        methodInstrumentables = children,
        parameters = parameters
    )

    override val children: List<MethodInstrumentable> = listOf(
        ExecuteInsert(),
        ExecuteUpdateDelete()
    )
}

class ExecuteInsert : MethodInstrumentable {
    override val fqName: String get() = "executeInsert"

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ExecuteStatementMethodVisitor(
        ReturnType.LONG,
        apiVersion,
        originalVisitor,
        instrumentableContext.access,
        instrumentableContext.descriptor
    )
}

class ExecuteUpdateDelete : MethodInstrumentable {
    override val fqName: String get() = "executeUpdateDelete"

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ExecuteStatementMethodVisitor(
        ReturnType.INTEGER,
        apiVersion,
        originalVisitor,
        instrumentableContext.access,
        instrumentableContext.descriptor
    )
}
