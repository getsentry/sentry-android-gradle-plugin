package androidx.sqlite.driver;

import androidx.sqlite.SQLiteDriver;

/** Minimal stub of the real, final Android driver. */
public final class AndroidSQLiteDriver implements SQLiteDriver {
  @Override
  public Object open(String fileName) {
    return null;
  }
}
