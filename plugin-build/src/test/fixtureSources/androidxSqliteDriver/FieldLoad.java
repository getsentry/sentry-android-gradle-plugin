package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

/** setDriver argument loaded from an instance field. */
public class FieldLoad {

  private BundledSQLiteDriver driver;

  public void build() {
    new RoomDatabase.Builder().setDriver(driver);
  }
}
