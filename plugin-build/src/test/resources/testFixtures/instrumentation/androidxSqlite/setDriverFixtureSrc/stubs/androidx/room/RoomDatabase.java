package androidx.room;
import androidx.sqlite.SQLiteDriver;
public class RoomDatabase {
  public static final class Builder {
    public Builder() {}
    public Builder setDriver(SQLiteDriver driver) { return this; }
  }
}
