@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.androidx.sqlite.database

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor.ExecSqlMethodVisitor
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.visitor.QueryMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteDatabase : ClassInstrumentable {
  override val fqName: String
    get() = "androidx.sqlite.db.framework.FrameworkSQLiteDatabase"

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor =
    CommonClassVisitor(
      apiVersion = apiVersion,
      classVisitor = originalVisitor,
      className = fqName.substringAfterLast('.'),
      methodInstrumentables = listOf(Query(), ExecSql()),
      parameters = parameters,
    )
}

class Query : MethodInstrumentable {
  override val fqName: String
    get() = "query"

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor =
    if (
      instrumentableContext.descriptor == QUERY_METHOD_DESCRIPTOR ||
        instrumentableContext.descriptor == QUERY_METHOD_WITH_CANCELLATION_DESCRIPTOR
    ) {
      // only instrument certain method overloads, as the other ones are calling these 2, otherwise
      // we'd be creating 2 spans for one method
      QueryMethodVisitor(
        api = apiVersion,
        originalVisitor = originalVisitor,
        access = instrumentableContext.access,
        descriptor = instrumentableContext.descriptor,
      )
    } else {
      originalVisitor
    }

  companion object {
    private const val QUERY_METHOD_DESCRIPTOR =
      "(Landroidx/sqlite/db/SupportSQLiteQuery;)Landroid/database/Cursor;"
    private const val QUERY_METHOD_WITH_CANCELLATION_DESCRIPTOR =
      "(Landroidx/sqlite/db/SupportSQLiteQuery;Landroid/os/CancellationSignal;)" +
        "Landroid/database/Cursor;"
  }
}

class ExecSql : MethodInstrumentable {
  override val fqName: String
    get() = "execSQL"

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor =
    ExecSqlMethodVisitor(
      api = apiVersion,
      originalVisitor = originalVisitor,
      access = instrumentableContext.access,
      descriptor = instrumentableContext.descriptor,
    )
}
