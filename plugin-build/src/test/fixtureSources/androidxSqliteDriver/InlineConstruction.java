package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

/** Driver constructed inline as the setDriver argument. */
public class InlineConstruction {

  public void build() {
    new RoomDatabase.Builder().setDriver(new BundledSQLiteDriver());
  }
}
