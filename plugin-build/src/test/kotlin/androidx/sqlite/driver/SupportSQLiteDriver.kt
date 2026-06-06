package androidx.sqlite.driver

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteOpenHelper

class SupportSQLiteDriver(val openHelper: SupportSQLiteOpenHelper) : SQLiteDriver
