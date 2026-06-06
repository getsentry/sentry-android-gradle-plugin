package com.example;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;
import io.sentry.sqlite.SentrySQLiteDriver;

/**
 * Already manually wrapped with SentrySQLiteDriver.create(). The plugin wraps unconditionally, so
 * the instrumented bytecode contains two create() calls — idempotency is enforced by the SDK, not
 * the visitor.
 */
public class ManualWrap {

  public void build() {
    new RoomDatabase.Builder().setDriver(SentrySQLiteDriver.create(new BundledSQLiteDriver()));
  }
}
