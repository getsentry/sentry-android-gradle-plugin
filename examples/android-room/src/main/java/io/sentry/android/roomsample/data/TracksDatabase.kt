package io.sentry.android.roomsample.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Artist::class,
        Album::class,
        Track::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TracksDatabase : RoomDatabase() {
    abstract fun tracksDao(): TracksDao
}
