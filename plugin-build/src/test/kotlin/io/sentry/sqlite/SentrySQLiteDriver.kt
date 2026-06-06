package io.sentry.sqlite

import androidx.sqlite.SQLiteDriver

class SentrySQLiteDriver private constructor(private val delegate: SQLiteDriver) : SQLiteDriver {
  companion object {
    @JvmStatic fun create(delegate: SQLiteDriver): SQLiteDriver = SentrySQLiteDriver(delegate)
  }
}
