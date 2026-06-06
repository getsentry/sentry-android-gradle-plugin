package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Wraps the SQLiteDriver argument of `RoomDatabase.Builder.setDriver(SQLiteDriver)` with
 * `SentrySQLiteDriver.create(...)` at the call site. Owner-agnostic: matches any
 * `setDriver(Landroidx/sqlite/SQLiteDriver;)...` invocation.
 *
 * The bridge case (the argument is an `androidx.sqlite.driver.SupportSQLiteDriver` wrapping a
 * Sentry-wrapped open helper) is defended in the SDK side of `SentrySQLiteDriver.create()`, not
 * here — the SDK skips wrapping the bridge class to avoid duplicate spans when both the helper and
 * the driver would otherwise be instrumented. Idempotency for re-wrapped drivers is also handled in
 * the SDK. The visitor therefore wraps every `setDriver` argument unconditionally.
 *
 * The injected INVOKESTATIC has net-zero stack effect (pops one SQLiteDriver, pushes one
 * SQLiteDriver), so existing stack-map frames remain valid; COMPUTE_MAXS is sufficient.
 */
class SQLiteDriverCallSiteVisitor(apiVersion: Int, originalVisitor: MethodVisitor) :
  MethodVisitor(apiVersion, originalVisitor) {

  override fun visitMethodInsn(
    opcode: Int,
    owner: String?,
    name: String?,
    descriptor: String?,
    isInterface: Boolean,
  ) {
    // setDriver is always an instance call; skip name/descriptor checks on other opcodes.
    if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      return
    }
    if (name == SET_DRIVER && descriptor?.startsWith(SET_DRIVER_DESCRIPTOR_PREFIX) == true) {
      super.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        SENTRY_SQLITE_DRIVER,
        CREATE,
        SENTRY_CREATE_DESCRIPTOR,
        false,
      )
    }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
  }

  companion object {
    private const val SET_DRIVER = "setDriver"
    private const val SET_DRIVER_DESCRIPTOR_PREFIX = "(Landroidx/sqlite/SQLiteDriver;)"
    private const val SENTRY_SQLITE_DRIVER = "io/sentry/sqlite/SentrySQLiteDriver"
    private const val CREATE = "create"
    private const val SENTRY_CREATE_DESCRIPTOR =
      "(Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;"
  }
}
