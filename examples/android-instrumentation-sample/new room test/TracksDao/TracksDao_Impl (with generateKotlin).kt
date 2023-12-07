package io.sentry.samples.instrumentation.`data`

import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.CoroutinesRoom
import androidx.room.CoroutinesRoom.Companion.execute
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Float
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
public class TracksDao_Impl(
  __db: RoomDatabase,
) : TracksDao() {
  private val __db: RoomDatabase

  private val __insertionAdapterOfTrack: EntityInsertionAdapter<Track>

  private val __deletionAdapterOfTrack: EntityDeletionOrUpdateAdapter<Track>

  private val __updateAdapterOfTrack: EntityDeletionOrUpdateAdapter<Track>
  init {
    this.__db = __db
    this.__insertionAdapterOfTrack = object : EntityInsertionAdapter<Track>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `Track` (`TrackId`,`Name`,`AlbumId`,`Composer`,`MediaTypeId`,`GenreId`,`Milliseconds`,`Bytes`,`UnitPrice`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: Track) {
        statement.bindLong(1, entity.id)
        statement.bindString(2, entity.name)
        val _tmpAlbumId: Long? = entity.albumId
        if (_tmpAlbumId == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpAlbumId)
        }
        val _tmpComposer: String? = entity.composer
        if (_tmpComposer == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpComposer)
        }
        val _tmpMediaTypeId: Long? = entity.mediaTypeId
        if (_tmpMediaTypeId == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpMediaTypeId)
        }
        val _tmpGenreId: Long? = entity.genreId
        if (_tmpGenreId == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpGenreId)
        }
        statement.bindLong(7, entity.millis)
        val _tmpBytes: Long? = entity.bytes
        if (_tmpBytes == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpBytes)
        }
        statement.bindDouble(9, entity.price.toDouble())
      }
    }
    this.__deletionAdapterOfTrack = object : EntityDeletionOrUpdateAdapter<Track>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `Track` WHERE `TrackId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: Track) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfTrack = object : EntityDeletionOrUpdateAdapter<Track>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR REPLACE `Track` SET `TrackId` = ?,`Name` = ?,`AlbumId` = ?,`Composer` = ?,`MediaTypeId` = ?,`GenreId` = ?,`Milliseconds` = ?,`Bytes` = ?,`UnitPrice` = ? WHERE `TrackId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: Track) {
        statement.bindLong(1, entity.id)
        statement.bindString(2, entity.name)
        val _tmpAlbumId: Long? = entity.albumId
        if (_tmpAlbumId == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpAlbumId)
        }
        val _tmpComposer: String? = entity.composer
        if (_tmpComposer == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpComposer)
        }
        val _tmpMediaTypeId: Long? = entity.mediaTypeId
        if (_tmpMediaTypeId == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpMediaTypeId)
        }
        val _tmpGenreId: Long? = entity.genreId
        if (_tmpGenreId == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpGenreId)
        }
        statement.bindLong(7, entity.millis)
        val _tmpBytes: Long? = entity.bytes
        if (_tmpBytes == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpBytes)
        }
        statement.bindDouble(9, entity.price.toDouble())
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun insert(track: Track): Long = CoroutinesRoom.execute(__db, true, object
      : Callable<Long> {
    public override fun call(): Long {
      __db.beginTransaction()
      try {
        val _result: Long = __insertionAdapterOfTrack.insertAndReturnId(track)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertAll(vararg tracks: Track): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfTrack.insert(tracks)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun delete(track: Track): Unit = CoroutinesRoom.execute(__db, true, object
      : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfTrack.handle(track)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun update(track: Track): Unit = CoroutinesRoom.execute(__db, true, object
      : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfTrack.handle(track)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun all(): Flow<List<Track>> {
    val _sql: String = "SELECT * FROM track ORDER BY TrackId DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("track"), object : Callable<List<Track>> {
      public override fun call(): List<Track> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "TrackId")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "Name")
          val _cursorIndexOfAlbumId: Int = getColumnIndexOrThrow(_cursor, "AlbumId")
          val _cursorIndexOfComposer: Int = getColumnIndexOrThrow(_cursor, "Composer")
          val _cursorIndexOfMediaTypeId: Int = getColumnIndexOrThrow(_cursor, "MediaTypeId")
          val _cursorIndexOfGenreId: Int = getColumnIndexOrThrow(_cursor, "GenreId")
          val _cursorIndexOfMillis: Int = getColumnIndexOrThrow(_cursor, "Milliseconds")
          val _cursorIndexOfBytes: Int = getColumnIndexOrThrow(_cursor, "Bytes")
          val _cursorIndexOfPrice: Int = getColumnIndexOrThrow(_cursor, "UnitPrice")
          val _result: MutableList<Track> = ArrayList<Track>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: Track
            val _tmpId: Long
            _tmpId = _cursor.getLong(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpAlbumId: Long?
            if (_cursor.isNull(_cursorIndexOfAlbumId)) {
              _tmpAlbumId = null
            } else {
              _tmpAlbumId = _cursor.getLong(_cursorIndexOfAlbumId)
            }
            val _tmpComposer: String?
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer)
            }
            val _tmpMediaTypeId: Long?
            if (_cursor.isNull(_cursorIndexOfMediaTypeId)) {
              _tmpMediaTypeId = null
            } else {
              _tmpMediaTypeId = _cursor.getLong(_cursorIndexOfMediaTypeId)
            }
            val _tmpGenreId: Long?
            if (_cursor.isNull(_cursorIndexOfGenreId)) {
              _tmpGenreId = null
            } else {
              _tmpGenreId = _cursor.getLong(_cursorIndexOfGenreId)
            }
            val _tmpMillis: Long
            _tmpMillis = _cursor.getLong(_cursorIndexOfMillis)
            val _tmpBytes: Long?
            if (_cursor.isNull(_cursorIndexOfBytes)) {
              _tmpBytes = null
            } else {
              _tmpBytes = _cursor.getLong(_cursorIndexOfBytes)
            }
            val _tmpPrice: Float
            _tmpPrice = _cursor.getFloat(_cursorIndexOfPrice)
            _item =
                Track(_tmpId,_tmpName,_tmpAlbumId,_tmpComposer,_tmpMediaTypeId,_tmpGenreId,_tmpMillis,_tmpBytes,_tmpPrice)
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

  public override fun allByArtist(bandName: String): List<Track> {
    val _sql: String =
        "SELECT * FROM Track WHERE AlbumId = (SELECT AlbumId FROM Album WHERE ArtistId = (SELECT ArtistId from Artist WHERE Name = ?))"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, bandName)
    __db.assertNotSuspendingTransaction()
    val _cursor: Cursor = query(__db, _statement, false, null)
    try {
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "TrackId")
      val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "Name")
      val _cursorIndexOfAlbumId: Int = getColumnIndexOrThrow(_cursor, "AlbumId")
      val _cursorIndexOfComposer: Int = getColumnIndexOrThrow(_cursor, "Composer")
      val _cursorIndexOfMediaTypeId: Int = getColumnIndexOrThrow(_cursor, "MediaTypeId")
      val _cursorIndexOfGenreId: Int = getColumnIndexOrThrow(_cursor, "GenreId")
      val _cursorIndexOfMillis: Int = getColumnIndexOrThrow(_cursor, "Milliseconds")
      val _cursorIndexOfBytes: Int = getColumnIndexOrThrow(_cursor, "Bytes")
      val _cursorIndexOfPrice: Int = getColumnIndexOrThrow(_cursor, "UnitPrice")
      val _result: MutableList<Track> = ArrayList<Track>(_cursor.getCount())
      while (_cursor.moveToNext()) {
        val _item: Track
        val _tmpId: Long
        _tmpId = _cursor.getLong(_cursorIndexOfId)
        val _tmpName: String
        _tmpName = _cursor.getString(_cursorIndexOfName)
        val _tmpAlbumId: Long?
        if (_cursor.isNull(_cursorIndexOfAlbumId)) {
          _tmpAlbumId = null
        } else {
          _tmpAlbumId = _cursor.getLong(_cursorIndexOfAlbumId)
        }
        val _tmpComposer: String?
        if (_cursor.isNull(_cursorIndexOfComposer)) {
          _tmpComposer = null
        } else {
          _tmpComposer = _cursor.getString(_cursorIndexOfComposer)
        }
        val _tmpMediaTypeId: Long?
        if (_cursor.isNull(_cursorIndexOfMediaTypeId)) {
          _tmpMediaTypeId = null
        } else {
          _tmpMediaTypeId = _cursor.getLong(_cursorIndexOfMediaTypeId)
        }
        val _tmpGenreId: Long?
        if (_cursor.isNull(_cursorIndexOfGenreId)) {
          _tmpGenreId = null
        } else {
          _tmpGenreId = _cursor.getLong(_cursorIndexOfGenreId)
        }
        val _tmpMillis: Long
        _tmpMillis = _cursor.getLong(_cursorIndexOfMillis)
        val _tmpBytes: Long?
        if (_cursor.isNull(_cursorIndexOfBytes)) {
          _tmpBytes = null
        } else {
          _tmpBytes = _cursor.getLong(_cursorIndexOfBytes)
        }
        val _tmpPrice: Float
        _tmpPrice = _cursor.getFloat(_cursorIndexOfPrice)
        _item =
            Track(_tmpId,_tmpName,_tmpAlbumId,_tmpComposer,_tmpMediaTypeId,_tmpGenreId,_tmpMillis,_tmpBytes,_tmpPrice)
        _result.add(_item)
      }
      return _result
    } finally {
      _cursor.close()
      _statement.release()
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM Track"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, true, _cancellationSignal, object : Callable<Int> {
      public override fun call(): Int {
        __db.beginTransaction()
        try {
          val _cursor: Cursor = query(__db, _statement, false, null)
          try {
            val _result: Int
            if (_cursor.moveToFirst()) {
              val _tmp: Int
              _tmp = _cursor.getInt(0)
              _result = _tmp
            } else {
              _result = 0
            }
            __db.setTransactionSuccessful()
            return _result
          } finally {
            _cursor.close()
            _statement.release()
          }
        } finally {
          __db.endTransaction()
        }
      }
    })
  }

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
