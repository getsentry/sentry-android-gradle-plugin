package io.sentry.samples.instrumentation.`data`

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.sentry.android.instrumentation.lib.`data`.FavoritesDao
import io.sentry.android.instrumentation.lib.`data`.FavoritesDao_Impl
import java.lang.Class
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.Boolean
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.Set

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class TracksDatabase_Impl : TracksDatabase() {
  private val _tracksDao: Lazy<TracksDao> = lazy {
    TracksDao_Impl(this)
  }


  private val _favoritesDao: Lazy<FavoritesDao> = lazy {
    FavoritesDao_Impl(this)
  }


  protected override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
    val _openCallback: SupportSQLiteOpenHelper.Callback = RoomOpenHelper(config, object :
        RoomOpenHelper.Delegate(1) {
      public override fun createAllTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Artist` (`ArtistId` INTEGER NOT NULL, `Name` TEXT, PRIMARY KEY(`ArtistId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Album` (`AlbumId` INTEGER NOT NULL, `Title` TEXT NOT NULL, `ArtistId` INTEGER NOT NULL, PRIMARY KEY(`AlbumId`), FOREIGN KEY(`ArtistId`) REFERENCES `Artist`(`ArtistId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Track` (`TrackId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `Name` TEXT NOT NULL, `AlbumId` INTEGER, `Composer` TEXT, `MediaTypeId` INTEGER, `GenreId` INTEGER, `Milliseconds` INTEGER NOT NULL, `Bytes` INTEGER, `UnitPrice` REAL NOT NULL, FOREIGN KEY(`AlbumId`) REFERENCES `Album`(`AlbumId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Favorite` (`FavoriteId` INTEGER NOT NULL, PRIMARY KEY(`FavoriteId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e0b32dd1ecda874f3798dc66abf104ab')")
      }

      public override fun dropAllTables(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `Artist`")
        db.execSQL("DROP TABLE IF EXISTS `Album`")
        db.execSQL("DROP TABLE IF EXISTS `Track`")
        db.execSQL("DROP TABLE IF EXISTS `Favorite`")
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onDestructiveMigration(db)
          }
        }
      }

      public override fun onCreate(db: SupportSQLiteDatabase) {
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onCreate(db)
          }
        }
      }

      public override fun onOpen(db: SupportSQLiteDatabase) {
        mDatabase = db
        db.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(db)
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onOpen(db)
          }
        }
      }

      public override fun onPreMigrate(db: SupportSQLiteDatabase) {
        dropFtsSyncTriggers(db)
      }

      public override fun onPostMigrate(db: SupportSQLiteDatabase) {
      }

      public override fun onValidateSchema(db: SupportSQLiteDatabase):
          RoomOpenHelper.ValidationResult {
        val _columnsArtist: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(2)
        _columnsArtist.put("ArtistId", TableInfo.Column("ArtistId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsArtist.put("Name", TableInfo.Column("Name", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysArtist: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesArtist: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoArtist: TableInfo = TableInfo("Artist", _columnsArtist, _foreignKeysArtist,
            _indicesArtist)
        val _existingArtist: TableInfo = read(db, "Artist")
        if (!_infoArtist.equals(_existingArtist)) {
          return RoomOpenHelper.ValidationResult(false, """
              |Artist(io.sentry.samples.instrumentation.data.Artist).
              | Expected:
              |""".trimMargin() + _infoArtist + """
              |
              | Found:
              |""".trimMargin() + _existingArtist)
        }
        val _columnsAlbum: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(3)
        _columnsAlbum.put("AlbumId", TableInfo.Column("AlbumId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlbum.put("Title", TableInfo.Column("Title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlbum.put("ArtistId", TableInfo.Column("ArtistId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAlbum: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(1)
        _foreignKeysAlbum.add(TableInfo.ForeignKey("Artist", "NO ACTION", "NO ACTION",
            listOf("ArtistId"), listOf("ArtistId")))
        val _indicesAlbum: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoAlbum: TableInfo = TableInfo("Album", _columnsAlbum, _foreignKeysAlbum,
            _indicesAlbum)
        val _existingAlbum: TableInfo = read(db, "Album")
        if (!_infoAlbum.equals(_existingAlbum)) {
          return RoomOpenHelper.ValidationResult(false, """
              |Album(io.sentry.samples.instrumentation.data.Album).
              | Expected:
              |""".trimMargin() + _infoAlbum + """
              |
              | Found:
              |""".trimMargin() + _existingAlbum)
        }
        val _columnsTrack: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(9)
        _columnsTrack.put("TrackId", TableInfo.Column("TrackId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("Name", TableInfo.Column("Name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("AlbumId", TableInfo.Column("AlbumId", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("Composer", TableInfo.Column("Composer", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("MediaTypeId", TableInfo.Column("MediaTypeId", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("GenreId", TableInfo.Column("GenreId", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("Milliseconds", TableInfo.Column("Milliseconds", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("Bytes", TableInfo.Column("Bytes", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrack.put("UnitPrice", TableInfo.Column("UnitPrice", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTrack: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(1)
        _foreignKeysTrack.add(TableInfo.ForeignKey("Album", "NO ACTION", "NO ACTION",
            listOf("AlbumId"), listOf("AlbumId")))
        val _indicesTrack: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoTrack: TableInfo = TableInfo("Track", _columnsTrack, _foreignKeysTrack,
            _indicesTrack)
        val _existingTrack: TableInfo = read(db, "Track")
        if (!_infoTrack.equals(_existingTrack)) {
          return RoomOpenHelper.ValidationResult(false, """
              |Track(io.sentry.samples.instrumentation.data.Track).
              | Expected:
              |""".trimMargin() + _infoTrack + """
              |
              | Found:
              |""".trimMargin() + _existingTrack)
        }
        val _columnsFavorite: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(1)
        _columnsFavorite.put("FavoriteId", TableInfo.Column("FavoriteId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFavorite: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesFavorite: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoFavorite: TableInfo = TableInfo("Favorite", _columnsFavorite, _foreignKeysFavorite,
            _indicesFavorite)
        val _existingFavorite: TableInfo = read(db, "Favorite")
        if (!_infoFavorite.equals(_existingFavorite)) {
          return RoomOpenHelper.ValidationResult(false, """
              |Favorite(io.sentry.android.instrumentation.lib.data.Favorite).
              | Expected:
              |""".trimMargin() + _infoFavorite + """
              |
              | Found:
              |""".trimMargin() + _existingFavorite)
        }
        return RoomOpenHelper.ValidationResult(true, null)
      }
    }, "e0b32dd1ecda874f3798dc66abf104ab", "8760bbccea3af62ea7b5d3eff227e266")
    val _sqliteConfig: SupportSQLiteOpenHelper.Configuration =
        SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build()
    val _helper: SupportSQLiteOpenHelper = config.sqliteOpenHelperFactory.create(_sqliteConfig)
    return _helper
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: HashMap<String, String> = HashMap<String, String>(0)
    val _viewTables: HashMap<String, Set<String>> = HashMap<String, Set<String>>(0)
    return InvalidationTracker(this, _shadowTablesMap, _viewTables,
        "Artist","Album","Track","Favorite")
  }

  public override fun clearAllTables() {
    super.assertNotMainThread()
    val _db: SupportSQLiteDatabase = super.openHelper.writableDatabase
    val _supportsDeferForeignKeys: Boolean = android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.LOLLIPOP
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE")
      }
      super.beginTransaction()
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE")
      }
      _db.execSQL("DELETE FROM `Track`")
      _db.execSQL("DELETE FROM `Album`")
      _db.execSQL("DELETE FROM `Artist`")
      _db.execSQL("DELETE FROM `Favorite`")
      super.setTransactionSuccessful()
    } finally {
      super.endTransaction()
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE")
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close()
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM")
      }
    }
  }

  protected override fun getRequiredTypeConverters(): Map<Class<out Any>, List<Class<out Any>>> {
    val _typeConvertersMap: HashMap<Class<out Any>, List<Class<out Any>>> =
        HashMap<Class<out Any>, List<Class<out Any>>>()
    _typeConvertersMap.put(TracksDao::class.java, TracksDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FavoritesDao::class.java, FavoritesDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: HashSet<Class<out AutoMigrationSpec>> =
        HashSet<Class<out AutoMigrationSpec>>()
    return _autoMigrationSpecsSet
  }

  public override
      fun getAutoMigrations(autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = ArrayList<Migration>()
    return _autoMigrations
  }

  public override fun tracksDao(): TracksDao = _tracksDao.value

  public override fun favoritesDao(): FavoritesDao = _favoritesDao.value
}
