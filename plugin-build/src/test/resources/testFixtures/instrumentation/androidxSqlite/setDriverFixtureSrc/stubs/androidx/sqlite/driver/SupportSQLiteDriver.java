package androidx.sqlite.driver;
import androidx.sqlite.SQLiteDriver;
// The bridge that adapts a SupportSQLiteOpenHelper into a SQLiteDriver. The real ctor takes a
// SupportSQLiteOpenHelper; the call-site decision keys only on the arg's static type, so a no-arg
// ctor here is sufficient to exercise the SKIP-the-bridge rule.
public final class SupportSQLiteDriver implements SQLiteDriver {
  public SupportSQLiteDriver() {}
}
