package io.sentry.samples.instrumentation.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TracksDao_Impl extends TracksDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Track> __insertionAdapterOfTrack;

  private final EntityDeletionOrUpdateAdapter<Track> __deletionAdapterOfTrack;

  private final EntityDeletionOrUpdateAdapter<Track> __updateAdapterOfTrack;

  public TracksDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrack = new EntityInsertionAdapter<Track>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `Track` (`TrackId`,`Name`,`AlbumId`,`Composer`,`MediaTypeId`,`GenreId`,`Milliseconds`,`Bytes`,`UnitPrice`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getAlbumId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getAlbumId());
        }
        if (entity.getComposer() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getComposer());
        }
        if (entity.getMediaTypeId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getMediaTypeId());
        }
        if (entity.getGenreId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getGenreId());
        }
        statement.bindLong(7, entity.getMillis());
        if (entity.getBytes() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getBytes());
        }
        statement.bindDouble(9, entity.getPrice());
      }
    };
    this.__deletionAdapterOfTrack = new EntityDeletionOrUpdateAdapter<Track>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Track` WHERE `TrackId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Track entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTrack = new EntityDeletionOrUpdateAdapter<Track>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Track` SET `TrackId` = ?,`Name` = ?,`AlbumId` = ?,`Composer` = ?,`MediaTypeId` = ?,`GenreId` = ?,`Milliseconds` = ?,`Bytes` = ?,`UnitPrice` = ? WHERE `TrackId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getAlbumId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getAlbumId());
        }
        if (entity.getComposer() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getComposer());
        }
        if (entity.getMediaTypeId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getMediaTypeId());
        }
        if (entity.getGenreId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getGenreId());
        }
        statement.bindLong(7, entity.getMillis());
        if (entity.getBytes() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getBytes());
        }
        statement.bindDouble(9, entity.getPrice());
        statement.bindLong(10, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final Track track, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTrack.insertAndReturnId(track);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final Track[] tracks, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrack.insert(tracks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Track track, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTrack.handle(track);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Track track, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTrack.handle(track);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Track>> all() {
    final String _sql = "SELECT * FROM track ORDER BY TrackId DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"track"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "TrackId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "Name");
          final int _cursorIndexOfAlbumId = CursorUtil.getColumnIndexOrThrow(_cursor, "AlbumId");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "Composer");
          final int _cursorIndexOfMediaTypeId = CursorUtil.getColumnIndexOrThrow(_cursor, "MediaTypeId");
          final int _cursorIndexOfGenreId = CursorUtil.getColumnIndexOrThrow(_cursor, "GenreId");
          final int _cursorIndexOfMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "Milliseconds");
          final int _cursorIndexOfBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "Bytes");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "UnitPrice");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpAlbumId;
            if (_cursor.isNull(_cursorIndexOfAlbumId)) {
              _tmpAlbumId = null;
            } else {
              _tmpAlbumId = _cursor.getLong(_cursorIndexOfAlbumId);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final Long _tmpMediaTypeId;
            if (_cursor.isNull(_cursorIndexOfMediaTypeId)) {
              _tmpMediaTypeId = null;
            } else {
              _tmpMediaTypeId = _cursor.getLong(_cursorIndexOfMediaTypeId);
            }
            final Long _tmpGenreId;
            if (_cursor.isNull(_cursorIndexOfGenreId)) {
              _tmpGenreId = null;
            } else {
              _tmpGenreId = _cursor.getLong(_cursorIndexOfGenreId);
            }
            final long _tmpMillis;
            _tmpMillis = _cursor.getLong(_cursorIndexOfMillis);
            final Long _tmpBytes;
            if (_cursor.isNull(_cursorIndexOfBytes)) {
              _tmpBytes = null;
            } else {
              _tmpBytes = _cursor.getLong(_cursorIndexOfBytes);
            }
            final float _tmpPrice;
            _tmpPrice = _cursor.getFloat(_cursorIndexOfPrice);
            _item = new Track(_tmpId,_tmpName,_tmpAlbumId,_tmpComposer,_tmpMediaTypeId,_tmpGenreId,_tmpMillis,_tmpBytes,_tmpPrice);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<Track> allByArtist(final String bandName) {
    final String _sql = "SELECT * FROM Track WHERE AlbumId = (SELECT AlbumId FROM Album WHERE ArtistId = (SELECT ArtistId from Artist WHERE Name = ?))";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, bandName);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "TrackId");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "Name");
      final int _cursorIndexOfAlbumId = CursorUtil.getColumnIndexOrThrow(_cursor, "AlbumId");
      final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "Composer");
      final int _cursorIndexOfMediaTypeId = CursorUtil.getColumnIndexOrThrow(_cursor, "MediaTypeId");
      final int _cursorIndexOfGenreId = CursorUtil.getColumnIndexOrThrow(_cursor, "GenreId");
      final int _cursorIndexOfMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "Milliseconds");
      final int _cursorIndexOfBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "Bytes");
      final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "UnitPrice");
      final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Track _item;
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        final Long _tmpAlbumId;
        if (_cursor.isNull(_cursorIndexOfAlbumId)) {
          _tmpAlbumId = null;
        } else {
          _tmpAlbumId = _cursor.getLong(_cursorIndexOfAlbumId);
        }
        final String _tmpComposer;
        if (_cursor.isNull(_cursorIndexOfComposer)) {
          _tmpComposer = null;
        } else {
          _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
        }
        final Long _tmpMediaTypeId;
        if (_cursor.isNull(_cursorIndexOfMediaTypeId)) {
          _tmpMediaTypeId = null;
        } else {
          _tmpMediaTypeId = _cursor.getLong(_cursorIndexOfMediaTypeId);
        }
        final Long _tmpGenreId;
        if (_cursor.isNull(_cursorIndexOfGenreId)) {
          _tmpGenreId = null;
        } else {
          _tmpGenreId = _cursor.getLong(_cursorIndexOfGenreId);
        }
        final long _tmpMillis;
        _tmpMillis = _cursor.getLong(_cursorIndexOfMillis);
        final Long _tmpBytes;
        if (_cursor.isNull(_cursorIndexOfBytes)) {
          _tmpBytes = null;
        } else {
          _tmpBytes = _cursor.getLong(_cursorIndexOfBytes);
        }
        final float _tmpPrice;
        _tmpPrice = _cursor.getFloat(_cursorIndexOfPrice);
        _item = new Track(_tmpId,_tmpName,_tmpAlbumId,_tmpComposer,_tmpMediaTypeId,_tmpGenreId,_tmpMillis,_tmpBytes,_tmpPrice);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM Track";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
          try {
            final Integer _result;
            if (_cursor.moveToFirst()) {
              final int _tmp;
              _tmp = _cursor.getInt(0);
              _result = _tmp;
            } else {
              _result = 0;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
