import androidx.room.RoomDatabase;
import androidx.sqlite.SQLiteDriver;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;
// SKIP case: arg static type is the erased SQLiteDriver interface (returned from a method) -> 0
// injected create. Bias to false-negative over double-wrap.
public class SetDriverBareInterface {
  public RoomDatabase.Builder build(RoomDatabase.Builder b) {
    return b.setDriver(provide());
  }
  private SQLiteDriver provide() {
    return new BundledSQLiteDriver();
  }
}
