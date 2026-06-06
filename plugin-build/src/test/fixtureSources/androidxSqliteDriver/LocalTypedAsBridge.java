package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.SQLiteDriver;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.driver.SupportSQLiteDriver;

/** Erased-local bridge: SQLiteDriver-typed local holding a SupportSQLiteDriver instance. */
public class LocalTypedAsBridge {

  private final SupportSQLiteOpenHelper helper;

  public LocalTypedAsBridge(SupportSQLiteOpenHelper helper) {
    this.helper = helper;
  }

  public void build() {
    SQLiteDriver driver = new SupportSQLiteDriver(helper);
    new RoomDatabase.Builder().setDriver(driver);
  }
}
