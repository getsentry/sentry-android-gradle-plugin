import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Kotlin reference showing the idiomatic equivalent of [InferredLocal.java]. The compiled `.class`
 * used by tests comes from the Java source; this file is kept for documentation only (it shows the
 * `val driver = BundledSQLiteDriver()` pattern that makes kotlinc emit a `checkcast` to
 * `SQLiteDriver` at the `setDriver` call site).
 */
class InferredLocal {
  fun configure(builder: RoomDatabase.Builder<RoomDatabase>) {
    val driver = BundledSQLiteDriver()
    builder.setDriver(driver)
  }
}
