package io.sentry.android.roomsample.data

import androidx.room.Database
import androidx.room.RoomDatabase
import io.sentry.android.roomsample.lib.data.Favorite
import io.sentry.android.roomsample.lib.data.FavoritesDao

@Database(
    entities = [
        Artist::class,
        Album::class,
        Track::class,
        Favorite::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TracksDatabase : RoomDatabase() {
    abstract fun tracksDao(): TracksDao
    abstract fun favoritesDao(): FavoritesDao
}
