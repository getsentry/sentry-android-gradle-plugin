package io.sentry.sqlite;

import androidx.room.RoomDatabase;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

/** Sentry-owned class with a setDriver call site — must not be instrumented by the plugin. */
public class SentrySetDriver {

  public void build() {
    new RoomDatabase.Builder().setDriver(new BundledSQLiteDriver());
  }
}
