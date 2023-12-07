package io.sentry.android.instrumentation.lib.`data`

import android.database.Cursor
import androidx.room.CoroutinesRoom
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class FavoritesDao_Impl(
  __db: RoomDatabase,
) : FavoritesDao() {
  private val __db: RoomDatabase

  private val __insertionAdapterOfFavorite: EntityInsertionAdapter<Favorite>

  private val __deletionAdapterOfFavorite: EntityDeletionOrUpdateAdapter<Favorite>

  private val __updateAdapterOfFavorite: EntityDeletionOrUpdateAdapter<Favorite>

  private val __preparedStmtOfCount: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfFavorite = object : EntityInsertionAdapter<Favorite>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `Favorite` (`FavoriteId`) VALUES (?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: Favorite) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__deletionAdapterOfFavorite = object : EntityDeletionOrUpdateAdapter<Favorite>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `Favorite` WHERE `FavoriteId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: Favorite) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfFavorite = object : EntityDeletionOrUpdateAdapter<Favorite>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR REPLACE `Favorite` SET `FavoriteId` = ? WHERE `FavoriteId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: Favorite) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.id)
      }
    }
    this.__preparedStmtOfCount = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM Favorite"
        return _query
      }
    }
  }

  public override suspend fun insert(favorite: Favorite): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfFavorite.insert(favorite)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertAll(vararg favorites: Favorite): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfFavorite.insert(favorites)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun delete(favorite: Favorite): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfFavorite.handle(favorite)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun update(favorite: Favorite): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfFavorite.handle(favorite)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun count(): Int = CoroutinesRoom.execute(__db, true, object :
      Callable<Int> {
    public override fun call(): Int {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfCount.acquire()
      try {
        __db.beginTransaction()
        try {
          val _result: Int = _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
          return _result
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfCount.release(_stmt)
      }
    }
  })

  public override fun all(): Flow<List<Favorite>> {
    val _sql: String = "SELECT * FROM Favorite ORDER BY FavoriteId DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("Favorite"), object :
        Callable<List<Favorite>> {
      public override fun call(): List<Favorite> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "FavoriteId")
          val _result: MutableList<Favorite> = ArrayList<Favorite>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: Favorite
            val _tmpId: Long
            _tmpId = _cursor.getLong(_cursorIndexOfId)
            _item = Favorite(_tmpId)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
        }
      }

      protected fun finalize() {
        _statement.release()
      }
    })
  }

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
