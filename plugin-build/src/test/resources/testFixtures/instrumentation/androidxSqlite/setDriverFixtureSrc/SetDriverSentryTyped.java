import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;
import io.sentry.sqlite.SentrySQLiteDriver;
// SKIP case: arg static type is the concrete io/sentry/sqlite/SentrySQLiteDriver -> 0 injected
// create. Unlike SetDriverAlreadySentry (whose arg erases to the bare SQLiteDriver interface
// because create() returns SQLiteDriver), this fixture exercises the dedicated SENTRY_DRIVER skip
// branch of SQLiteDriverMethodVisitor.isWrappable, guarding against an "already wrapped" arg that
// is statically typed as SentrySQLiteDriver getting double-wrapped.
public class SetDriverSentryTyped {
  public RoomDatabase.Builder build(RoomDatabase.Builder b) {
    return b.setDriver(SentrySQLiteDriver.createSentry(new BundledSQLiteDriver()));
  }
}
