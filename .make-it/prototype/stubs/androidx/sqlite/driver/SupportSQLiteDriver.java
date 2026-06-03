package androidx.sqlite.driver;

import androidx.sqlite.SQLiteDriver;

/**
 * Minimal stub of the bridge adapter. The real one wraps a SupportSQLiteOpenHelper; we use Object
 * to avoid pulling in the whole support stack. THIS is the class the plugin must never wrap.
 */
public final class SupportSQLiteDriver implements SQLiteDriver {
  private final Object helper;

  public SupportSQLiteDriver(Object helper) {
    this.helper = helper;
  }

  @Override
  public Object open(String fileName) {
    return null;
  }
}
