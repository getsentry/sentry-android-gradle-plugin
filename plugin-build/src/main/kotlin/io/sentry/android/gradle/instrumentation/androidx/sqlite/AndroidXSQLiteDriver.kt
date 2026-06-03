package io.sentry.android.gradle.instrumentation.androidx.sqlite

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.visitor.SQLiteDriverMethodVisitor
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

/**
 * Finds `RoomDatabase.Builder.setDriver(SQLiteDriver)` call sites across all (non-Sentry) app
 * classes and wraps the driver argument with `io.sentry.sqlite.SentrySQLiteDriver.create(...)`.
 *
 * A `setDriver` call can appear in any user class and [ClassContext]/`ClassData` has no method
 * bodies, so — like the File-I/O `WrappingInstrumentable` — this targets broadly
 * (`!isSentryClass()`) and does the actual per-instruction filtering in
 * [SQLiteDriverMethodVisitor].
 */
class AndroidXSQLiteDriver : ClassInstrumentable {

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor {
    val className = instrumentableContext.currentClassData.className
    return CommonClassVisitor(
      apiVersion = apiVersion,
      classVisitor = originalVisitor,
      className = className.substringAfterLast('.'),
      methodInstrumentables = listOf(SQLiteDriverMethodInstrumentable(className.replace('.', '/'))),
      parameters = parameters,
    )
  }

  override fun isInstrumentable(data: ClassContext): Boolean = !data.isSentryClass()
}

class SQLiteDriverMethodInstrumentable(private val owner: String) : MethodInstrumentable {

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor =
    SQLiteDriverMethodVisitor(apiVersion, originalVisitor, instrumentableContext, owner)

  // Match all methods; SQLiteDriverMethodVisitor filters per-instruction for setDriver call sites.
  override fun isInstrumentable(data: MethodContext): Boolean = true
}
