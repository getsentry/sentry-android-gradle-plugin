package androidx.room;

import androidx.sqlite.SQLiteDriver;

/** Minimal stub of androidx.room.RoomDatabase with the nested Builder.setDriver(SQLiteDriver). */
public class RoomDatabase {
  public static class Builder {
    public Builder setDriver(SQLiteDriver driver) {
      System.out.println(
          "    setDriver(...) received runtime type = "
              + (driver == null ? "null" : driver.getClass().getName()));
      return this;
    }
  }
}
