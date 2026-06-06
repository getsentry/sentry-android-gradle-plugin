package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.driver.SupportSQLiteDriver;

/** Bridge constructed inline: a SupportSQLiteDriver wrapping a SupportSQLiteOpenHelper. */
public class InlineBridge {

  private SupportSQLiteOpenHelper helper;

  public void build() {
    new RoomDatabase.Builder().setDriver(new SupportSQLiteDriver(helper));
  }
}
