package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.SQLiteDriver;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

/**
 * A {@code checkcast} to {@code SQLiteDriver} sits immediately before the {@code setDriver} call
 * site (here forced via an {@code Object}-typed local). This mirrors the bytecode kotlinc emits for
 * an inferred-type local and verifies the visitor still inserts the wrap when a {@code checkcast}
 * precedes the invocation.
 */
public class InferredLocal {

  public void build() {
    Object driver = new BundledSQLiteDriver();
    new RoomDatabase.Builder().setDriver((SQLiteDriver) driver);
  }
}
