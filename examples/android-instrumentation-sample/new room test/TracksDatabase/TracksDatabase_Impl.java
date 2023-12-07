package io.sentry.samples.instrumentation.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import io.sentry.android.instrumentation.lib.data.FavoritesDao;
import io.sentry.android.instrumentation.lib.data.FavoritesDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TracksDatabase_Impl extends TracksDatabase {
  private volatile TracksDao _tracksDao;

  private volatile FavoritesDao _favoritesDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Artist` (`ArtistId` INTEGER NOT NULL, `Name` TEXT, PRIMARY KEY(`ArtistId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `Album` (`AlbumId` INTEGER NOT NULL, `Title` TEXT NOT NULL, `ArtistId` INTEGER NOT NULL, PRIMARY KEY(`AlbumId`), FOREIGN KEY(`ArtistId`) REFERENCES `Artist`(`ArtistId`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `Track` (`TrackId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `Name` TEXT NOT NULL, `AlbumId` INTEGER, `Composer` TEXT, `MediaTypeId` INTEGER, `GenreId` INTEGER, `Milliseconds` INTEGER NOT NULL, `Bytes` INTEGER, `UnitPrice` REAL NOT NULL, FOREIGN KEY(`AlbumId`) REFERENCES `Album`(`AlbumId`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `Favorite` (`FavoriteId` INTEGER NOT NULL, PRIMARY KEY(`FavoriteId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e0b32dd1ecda874f3798dc66abf104ab')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `Artist`");
        db.execSQL("DROP TABLE IF EXISTS `Album`");
        db.execSQL("DROP TABLE IF EXISTS `Track`");
        db.execSQL("DROP TABLE IF EXISTS `Favorite`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsArtist = new HashMap<String, TableInfo.Column>(2);
        _columnsArtist.put("ArtistId", new TableInfo.Column("ArtistId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArtist.put("Name", new TableInfo.Column("Name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysArtist = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesArtist = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoArtist = new TableInfo("Artist", _columnsArtist, _foreignKeysArtist, _indicesArtist);
        final TableInfo _existingArtist = TableInfo.read(db, "Artist");
        if (!_infoArtist.equals(_existingArtist)) {
          return new RoomOpenHelper.ValidationResult(false, "Artist(io.sentry.samples.instrumentation.data.Artist).\n"
                  + " Expected:\n" + _infoArtist + "\n"
                  + " Found:\n" + _existingArtist);
        }
        final HashMap<String, TableInfo.Column> _columnsAlbum = new HashMap<String, TableInfo.Column>(3);
        _columnsAlbum.put("AlbumId", new TableInfo.Column("AlbumId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlbum.put("Title", new TableInfo.Column("Title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlbum.put("ArtistId", new TableInfo.Column("ArtistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAlbum = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAlbum.add(new TableInfo.ForeignKey("Artist", "NO ACTION", "NO ACTION", Arrays.asList("ArtistId"), Arrays.asList("ArtistId")));
        final HashSet<TableInfo.Index> _indicesAlbum = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAlbum = new TableInfo("Album", _columnsAlbum, _foreignKeysAlbum, _indicesAlbum);
        final TableInfo _existingAlbum = TableInfo.read(db, "Album");
        if (!_infoAlbum.equals(_existingAlbum)) {
          return new RoomOpenHelper.ValidationResult(false, "Album(io.sentry.samples.instrumentation.data.Album).\n"
                  + " Expected:\n" + _infoAlbum + "\n"
                  + " Found:\n" + _existingAlbum);
        }
        final HashMap<String, TableInfo.Column> _columnsTrack = new HashMap<String, TableInfo.Column>(9);
        _columnsTrack.put("TrackId", new TableInfo.Column("TrackId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("Name", new TableInfo.Column("Name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("AlbumId", new TableInfo.Column("AlbumId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("Composer", new TableInfo.Column("Composer", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("MediaTypeId", new TableInfo.Column("MediaTypeId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("GenreId", new TableInfo.Column("GenreId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("Milliseconds", new TableInfo.Column("Milliseconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("Bytes", new TableInfo.Column("Bytes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("UnitPrice", new TableInfo.Column("UnitPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrack = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysTrack.add(new TableInfo.ForeignKey("Album", "NO ACTION", "NO ACTION", Arrays.asList("AlbumId"), Arrays.asList("AlbumId")));
        final HashSet<TableInfo.Index> _indicesTrack = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrack = new TableInfo("Track", _columnsTrack, _foreignKeysTrack, _indicesTrack);
        final TableInfo _existingTrack = TableInfo.read(db, "Track");
        if (!_infoTrack.equals(_existingTrack)) {
          return new RoomOpenHelper.ValidationResult(false, "Track(io.sentry.samples.instrumentation.data.Track).\n"
                  + " Expected:\n" + _infoTrack + "\n"
                  + " Found:\n" + _existingTrack);
        }
        final HashMap<String, TableInfo.Column> _columnsFavorite = new HashMap<String, TableInfo.Column>(1);
        _columnsFavorite.put("FavoriteId", new TableInfo.Column("FavoriteId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFavorite = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFavorite = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFavorite = new TableInfo("Favorite", _columnsFavorite, _foreignKeysFavorite, _indicesFavorite);
        final TableInfo _existingFavorite = TableInfo.read(db, "Favorite");
        if (!_infoFavorite.equals(_existingFavorite)) {
          return new RoomOpenHelper.ValidationResult(false, "Favorite(io.sentry.android.instrumentation.lib.data.Favorite).\n"
                  + " Expected:\n" + _infoFavorite + "\n"
                  + " Found:\n" + _existingFavorite);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e0b32dd1ecda874f3798dc66abf104ab", "8760bbccea3af62ea7b5d3eff227e266");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "Artist","Album","Track","Favorite");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `Track`");
      _db.execSQL("DELETE FROM `Album`");
      _db.execSQL("DELETE FROM `Artist`");
      _db.execSQL("DELETE FROM `Favorite`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TracksDao.class, TracksDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FavoritesDao.class, FavoritesDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TracksDao tracksDao() {
    if (_tracksDao != null) {
      return _tracksDao;
    } else {
      synchronized(this) {
        if(_tracksDao == null) {
          _tracksDao = new TracksDao_Impl(this);
        }
        return _tracksDao;
      }
    }
  }

  @Override
  public FavoritesDao favoritesDao() {
    if (_favoritesDao != null) {
      return _favoritesDao;
    } else {
      synchronized(this) {
        if(_favoritesDao == null) {
          _favoritesDao = new FavoritesDao_Impl(this);
        }
        return _favoritesDao;
      }
    }
  }
}
