import androidx.room.RoomDatabase;
import androidx.sqlite.driver.AndroidSQLiteDriver;
// WRAP case: arg comes from a concrete-typed local (AndroidSQLiteDriver) -> 1 injected create.
// This is the case that would break a constructor-site wrap (SentrySQLiteDriver is not a subtype of
// AndroidSQLiteDriver) but is type-safe when wrapping the interface-typed argument.
public class SetDriverConcreteLocal {
  public RoomDatabase.Builder build(RoomDatabase.Builder b) {
    AndroidSQLiteDriver d = new AndroidSQLiteDriver();
    return b.setDriver(d);
  }
}
