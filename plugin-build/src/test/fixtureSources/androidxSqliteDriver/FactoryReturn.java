package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.SQLiteDriver;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

public class FactoryReturn {

  public void build() {
    new RoomDatabase.Builder().setDriver(provideDriver());
  }

  private SQLiteDriver provideDriver() {
    return new BundledSQLiteDriver();
  }
}
