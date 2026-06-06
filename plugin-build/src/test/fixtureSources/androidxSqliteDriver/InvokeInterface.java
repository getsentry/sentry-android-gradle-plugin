package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

public class InvokeInterface {

  public void build() {
    RoomDatabase.SetDriverReceiver receiver = new RoomDatabase.Builder();
    receiver.setDriver(new BundledSQLiteDriver());
  }
}
