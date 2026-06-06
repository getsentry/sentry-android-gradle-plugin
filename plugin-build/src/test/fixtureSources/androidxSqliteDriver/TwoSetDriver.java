package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

public class TwoSetDriver {

  public void build() {
    BundledSQLiteDriver first = new BundledSQLiteDriver();
    BundledSQLiteDriver second = new BundledSQLiteDriver();
    new RoomDatabase.Builder().setDriver(first);
    new RoomDatabase.Builder().setDriver(second);
  }
}
