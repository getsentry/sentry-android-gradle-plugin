package androidx.sqlite.driver

import androidx.sqlite.SQLiteDriver

/**
 * Mock of androidx.sqlite.driver.SupportSQLiteDriver, the bridge over a SupportSQLiteOpenHelper.
 * This is the one driver we must NEVER auto-wrap (the no-double-wrap guarantee) -> SKIP target.
 */
class SupportSQLiteDriver : SQLiteDriver
