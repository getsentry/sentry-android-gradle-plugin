import androidx.room.RoomDatabase;
import androidx.sqlite.driver.SupportSQLiteDriver;
// SKIP case (the hard no-double-wrap guarantee): arg static type is the SupportSQLiteDriver bridge
// -> 0 injected create.
public class SetDriverBridge {
  public RoomDatabase.Builder build(RoomDatabase.Builder b) {
    return b.setDriver(new SupportSQLiteDriver());
  }
}
