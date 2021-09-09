package io.sentry.android.gradle.instrumentation.androidx.sqlite.statement

import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.Instrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.visitor.ExecuteInsertMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteStatement : Instrumentable<ClassVisitor> {

    override val fqName: String get() = "androidx.sqlite.db.framework.FrameworkSQLiteStatement"

    override fun getVisitor(
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        descriptor: String?,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = fqName.substringAfterLast('.'),
        methodInstrumentables = children,
        parameters = parameters
    )

    override val children: List<Instrumentable<MethodVisitor>> = listOf(
        ExecuteInsert()
    )
}

class ExecuteInsert : Instrumentable<MethodVisitor> {
    override val fqName: String get() = "executeInsert"

    override fun getVisitor(
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        descriptor: String?,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ExecuteInsertMethodVisitor(apiVersion, originalVisitor)
}

