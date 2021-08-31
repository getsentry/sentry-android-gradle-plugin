package io.sentry.android.roomsample

import android.app.Application
import androidx.room.Room
import io.sentry.android.roomsample.data.TracksDatabase

class SampleApp : Application() {

    companion object {
        lateinit var database: TracksDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, TracksDatabase::class.java, "tracks.db")
            .createFromAsset("tracks.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
