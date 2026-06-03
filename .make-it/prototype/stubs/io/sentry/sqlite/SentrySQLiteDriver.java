package io.sentry.sqlite;

import androidx.sqlite.SQLiteDriver;

/**
 * Stub mirroring the real SentrySQLiteDriver.create() contract from sentry-java PR #5466: static
 * create(SQLiteDriver): SQLiteDriver, idempotent for an already-SentrySQLiteDriver delegate.
 */
public final class SentrySQLiteDriver implements SQLiteDriver {
  private final SQLiteDriver delegate;

  private SentrySQLiteDriver(SQLiteDriver delegate) {
    this.delegate = delegate;
  }

  @Override
  public Object open(String fileName) {
    return delegate.open(fileName);
  }

  public static SQLiteDriver create(SQLiteDriver delegate) {
    System.out.println("    >> SentrySQLiteDriver.create() wrapping " + delegate.getClass().getName());
    // idempotency: re-wrapping a SentrySQLiteDriver is a no-op
    if (delegate instanceof SentrySQLiteDriver) {
      return delegate;
    }
    return new SentrySQLiteDriver(delegate);
  }
}
