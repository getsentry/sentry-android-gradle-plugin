package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

/** Local typed as the concrete BundledSQLiteDriver impl, not the SQLiteDriver interface. */
public class LocalTypedAsImpl {

  public void build() {
    BundledSQLiteDriver driver = new BundledSQLiteDriver();
    new RoomDatabase.Builder().setDriver(driver);
  }
}
