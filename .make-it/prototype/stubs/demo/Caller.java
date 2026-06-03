package demo;

import androidx.room.RoomDatabase;
import androidx.sqlite.SQLiteDriver;
import androidx.sqlite.driver.AndroidSQLiteDriver;
import androidx.sqlite.driver.SupportSQLiteDriver;
import io.sentry.sqlite.SentrySQLiteDriver;

/** Exercises the idioms the call-site transformer must handle. Compiled, then bytecode-rewritten. */
public class Caller {

  // A: inline real driver -> EXPECT WRAP
  public void scenarioA_inlineRealDriver() {
    new RoomDatabase.Builder().setDriver(new AndroidSQLiteDriver());
  }

  // B: inline bridge -> EXPECT SKIP (no double-wrap)
  public void scenarioB_inlineBridge() {
    new RoomDatabase.Builder().setDriver(new SupportSQLiteDriver(new Object()));
  }

  // C: concrete-typed local real driver -> EXPECT WRAP
  public void scenarioC_localRealDriver() {
    AndroidSQLiteDriver d = new AndroidSQLiteDriver();
    new RoomDatabase.Builder().setDriver(d);
  }

  // D: interface-typed local holding a bridge -> EXPECT SKIP
  public void scenarioD_erasedLocalBridge() {
    SQLiteDriver d = new SupportSQLiteDriver(new Object());
    new RoomDatabase.Builder().setDriver(d);
  }

  // E: developer already manually wrapped -> EXPECT SKIP (arg type is the SQLiteDriver interface
  //    returned by create(); SDK idempotency would also backstop)
  public void scenarioE_manualWrap() {
    new RoomDatabase.Builder().setDriver(SentrySQLiteDriver.create(new AndroidSQLiteDriver()));
  }

  // F: factory method returning SQLiteDriver -> EXPECT SKIP (erased interface; bias to false-neg)
  public void scenarioF_factoryReturn() {
    new RoomDatabase.Builder().setDriver(provideDriver());
  }

  private SQLiteDriver provideDriver() {
    return new AndroidSQLiteDriver();
  }
}
