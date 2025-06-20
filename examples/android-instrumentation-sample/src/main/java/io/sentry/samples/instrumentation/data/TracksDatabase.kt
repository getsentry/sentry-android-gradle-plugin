package io.sentry.samples.instrumentation.data

import androidx.room.Database
import androidx.room.RoomDatabase
import io.sentry.android.instrumentation.lib.data.Favorite
import io.sentry.android.instrumentation.lib.data.FavoritesDao

@Database(
  entities = [Artist::class, Album::class, Track::class, Favorite::class],
  version = 1,
  exportSchema = false,
)
abstract class TracksDatabase : RoomDatabase() {
  abstract fun tracksDao(): TracksDao

  abstract fun favoritesDao(): FavoritesDao
}
