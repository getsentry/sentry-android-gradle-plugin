package io.sentry.sqlite;
import androidx.sqlite.SQLiteDriver;
public final class SentrySQLiteDriver implements SQLiteDriver {
  private SentrySQLiteDriver() {}
  public static SQLiteDriver create(SQLiteDriver delegate) { return delegate; }
  // Compile-only factory: lets a fixture hold a value whose *static type* is the concrete
  // SentrySQLiteDriver (not the bare SQLiteDriver interface), so the SENTRY_DRIVER skip branch of
  // SQLiteDriverMethodVisitor.isWrappable is exercised distinctly. Body never runs at test time.
  public static SentrySQLiteDriver createSentry(SQLiteDriver delegate) { return null; }
}
