package io.sentry.sqlite;
import androidx.sqlite.SQLiteDriver;
public final class SentrySQLiteDriver implements SQLiteDriver {
  private SentrySQLiteDriver() {}
  public static SQLiteDriver create(SQLiteDriver delegate) { return delegate; }
}
