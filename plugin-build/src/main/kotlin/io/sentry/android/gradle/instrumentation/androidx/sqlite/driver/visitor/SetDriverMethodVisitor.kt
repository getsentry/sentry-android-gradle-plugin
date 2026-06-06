package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

class SetDriverMethodVisitor(
  apiVersion: Int,
  originalVisitor: MethodVisitor,
  instrumentableContext: MethodContext,
) :
  AdviceAdapter(
    apiVersion,
    originalVisitor,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor,
  ) {

  override fun onMethodEnter() {
    loadArg(0)
    invokeStatic(Type.getType(SENTRY_SQLITE_DRIVER_TYPE), Method(CREATE, SENTRY_CREATE_DESCRIPTOR))
    storeArg(0)
  }

  companion object {
    internal const val CREATE = "create"
    internal const val SENTRY_CREATE_DESCRIPTOR =
      "(Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;"
    internal const val SENTRY_SQLITE_DRIVER_TYPE = "Lio/sentry/sqlite/SentrySQLiteDriver;"
  }
}
