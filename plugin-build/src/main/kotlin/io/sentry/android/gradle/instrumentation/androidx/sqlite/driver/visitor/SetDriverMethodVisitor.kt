package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.SQLITE_DRIVER_TYPE_DESCRIPTOR
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
    // We inject our wrapper at the start of RoomDatabase.Builder.setDriver(driver) so that the
    // method receives SentrySQLiteDriver.create(driver) instead of the raw driver (arg0).
    loadArg(0)
    invokeStatic(Type.getType(SENTRY_SQLITE_DRIVER_TYPE), Method(CREATE, SENTRY_CREATE_DESCRIPTOR))
    storeArg(0)
  }

  companion object {
    internal const val CREATE = "create"
    // Must match SentrySQLiteDriver.create(SQLiteDriver): SQLiteDriver in sentry-android-sqlite:
    // https://github.com/getsentry/sentry-java/blob/main/sentry-android-sqlite/src/main/java/io/sentry/sqlite/SentrySQLiteDriver.kt
    internal const val SENTRY_CREATE_DESCRIPTOR =
      "($SQLITE_DRIVER_TYPE_DESCRIPTOR)$SQLITE_DRIVER_TYPE_DESCRIPTOR"
    internal const val SENTRY_SQLITE_DRIVER_TYPE = "Lio/sentry/sqlite/SentrySQLiteDriver;"
  }
}
