import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;
// WRAP case: arg static type is the concrete BundledSQLiteDriver -> 1 injected create.
public class SetDriverConcrete {
  public RoomDatabase.Builder build(RoomDatabase.Builder b) {
    return b.setDriver(new BundledSQLiteDriver());
  }
}
