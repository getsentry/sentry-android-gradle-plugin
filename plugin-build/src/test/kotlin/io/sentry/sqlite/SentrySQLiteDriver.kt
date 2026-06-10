package io.sentry.sqlite

import androidx.sqlite.SQLiteDriver

/**
 * Minimal stub of `io.sentry.sqlite.SentrySQLiteDriver` so ASM can resolve the `INVOKESTATIC
 * io/sentry/sqlite/SentrySQLiteDriver.create` emitted by [SetDriverMethodVisitor]. The `@JvmStatic
 * create(SQLiteDriver)` shape here mirrors the SDK contract the visitor depends on.
 */
class SentrySQLiteDriver private constructor(private val delegate: SQLiteDriver) : SQLiteDriver {
  companion object {
    @JvmStatic fun create(delegate: SQLiteDriver): SQLiteDriver = SentrySQLiteDriver(delegate)
  }
}
