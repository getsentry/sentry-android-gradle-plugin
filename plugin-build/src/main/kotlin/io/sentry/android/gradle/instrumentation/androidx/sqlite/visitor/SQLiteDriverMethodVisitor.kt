package io.sentry.android.gradle.instrumentation.androidx.sqlite.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AnalyzerAdapter

/**
 * Wraps the argument of `RoomDatabase.Builder.setDriver(SQLiteDriver)` call sites with
 * `io.sentry.sqlite.SentrySQLiteDriver.create(...)`, so apps that opt into the Room driver API get
 * automatic SQL performance spans with zero config (the same experience the open-helper
 * instrumentation already provides).
 *
 * Unlike the other call-site rewriters in this plugin, this visitor extends ASM's [AnalyzerAdapter]
 * (the only usage of it in this codebase). [AnalyzerAdapter] tracks the operand-stack types so
 * that, just before a matching `setDriver` call, we can read the static type of the driver argument
 * (top of stack) and decide whether to wrap it. This is what lets us skip the `SupportSQLiteDriver`
 * bridge — the no-double-wrap guarantee.
 *
 * We wrap the *argument* (which occupies the interface-typed `SQLiteDriver` slot) rather than the
 * driver *construction*: `SentrySQLiteDriver` is `final` and only implements `SQLiteDriver`, so it
 * is not a subtype of the (also `final`) concrete drivers. Replacing a driver value held in a
 * concrete-typed local would therefore fail JVM verification; wrapping the interface-typed argument
 * is type-safe in every idiom (validated by the prototype in `.make-it/prototype/`).
 *
 * The injection is net-zero stack effect — it pops one `SQLiteDriver` and pushes one — so existing
 * stack-map frames stay valid (`CheckClassAdapter` passes with `COMPUTE_MAXS` alone).
 */
class SQLiteDriverMethodVisitor(
  apiVersion: Int,
  originalVisitor: MethodVisitor,
  instrumentableContext: MethodContext,
  owner: String,
) :
  AnalyzerAdapter(
    apiVersion,
    owner,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor,
    originalVisitor,
  ) {

  override fun visitMethodInsn(
    opcode: Int,
    owner: String?,
    name: String?,
    descriptor: String?,
    isInterface: Boolean,
  ) {
    // Owner-agnostic match: any `setDriver(SQLiteDriver)` call. This covers both
    // androidx/room/RoomDatabase$Builder (Room 2.7) and androidx/room3/RoomDatabase$Builder
    // (Room 3.0) without enumerating the Builder owners.
    val isSetDriver =
      name == "setDriver" && descriptor != null && descriptor.startsWith("(L$DRIVER_IFACE;)")
    if (isSetDriver) {
      // `stack` is maintained by AnalyzerAdapter and reflects the state *before* this instruction,
      // so the top of stack is the driver argument we are about to pass to setDriver.
      val topType = stack?.lastOrNull() as? String
      if (isWrappable(topType)) {
        super.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          SENTRY_DRIVER,
          "create",
          "(L$DRIVER_IFACE;)L$DRIVER_IFACE;",
          false,
        )
      }
    }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
  }

  private fun isWrappable(type: String?): Boolean =
    when (type) {
      null -> false
      // Never wrap the bridge -> the no-double-wrap guarantee. The bridge adapts a (possibly
      // already Sentry-wrapped) SupportSQLiteOpenHelper, so wrapping it would produce duplicate
      // spans.
      BRIDGE -> false
      // Already a Sentry driver (SDK create() is also idempotent for this, but skip eagerly).
      SENTRY_DRIVER -> false
      // Erased to the bare interface: we cannot prove it isn't a bridge, so bias to a
      // false-negative (a missed span) over a false-positive (a double span).
      DRIVER_IFACE -> false
      // Any other concrete type assignable to SQLiteDriver.
      else -> true
    }

  companion object {
    private const val DRIVER_IFACE = "androidx/sqlite/SQLiteDriver"
    private const val BRIDGE = "androidx/sqlite/driver/SupportSQLiteDriver"
    private const val SENTRY_DRIVER = "io/sentry/sqlite/SentrySQLiteDriver"
  }
}
