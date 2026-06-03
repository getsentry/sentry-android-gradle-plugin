package io.sentry.sqlite

import androidx.sqlite.SQLiteDriver

/**
 * Mock of io.sentry.sqlite.SentrySQLiteDriver. Only the static factory the instrumentation injects
 * needs to exist for verification:
 *
 * `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create
 * (Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;`
 */
class SentrySQLiteDriver private constructor() : SQLiteDriver {
  companion object {
    @JvmStatic fun create(delegate: SQLiteDriver): SQLiteDriver = delegate
  }
}
