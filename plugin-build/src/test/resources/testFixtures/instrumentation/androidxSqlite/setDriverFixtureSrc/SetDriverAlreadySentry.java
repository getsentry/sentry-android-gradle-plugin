import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;
import io.sentry.sqlite.SentrySQLiteDriver;
// SKIP case: arg is the result of SentrySQLiteDriver.create(...), whose static type is the bare
// SQLiteDriver interface -> 0 injected create (no double-wrap).
public class SetDriverAlreadySentry {
  public RoomDatabase.Builder build(RoomDatabase.Builder b) {
    return b.setDriver(SentrySQLiteDriver.create(new BundledSQLiteDriver()));
  }
}
