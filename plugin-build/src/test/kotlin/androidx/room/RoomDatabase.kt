package androidx.room

import androidx.sqlite.SQLiteDriver

/**
 * Mock of androidx.room.RoomDatabase with the nested Builder.setDriver(SQLiteDriver) used to
 * compile the setDriver call-site fixtures.
 */
open class RoomDatabase {
  class Builder {
    fun setDriver(driver: SQLiteDriver): Builder = this
  }
}
