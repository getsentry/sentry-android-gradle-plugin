package androidx.room

import androidx.sqlite.SQLiteDriver

abstract class RoomDatabase {
  interface SetDriverReceiver {
    fun setDriver(driver: SQLiteDriver): Builder<*>
  }

  open class Builder<T : RoomDatabase> : SetDriverReceiver {
    override fun setDriver(driver: SQLiteDriver): Builder<T> = this
  }
}
