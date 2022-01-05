package io.sentry.android.roomsample

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import io.sentry.android.roomsample.data.TracksDatabase
import io.sentry.android.roomsample.util.DEFAULT_LYRICS
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SampleApp : Application() {

    companion object {
        lateinit var database: TracksDatabase
            private set

        lateinit var analytics: SharedPreferences
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, TracksDatabase::class.java, "tracks.db")
            .createFromAsset("tracks.db")
            .fallbackToDestructiveMigration()
            .build()

        analytics = getSharedPreferences("analytics", Context.MODE_PRIVATE)

        GlobalScope.launch(Dispatchers.IO) {
            database.tracksDao().all()
                .collect { tracks ->
                    tracks.forEachIndexed { index, track ->
                        // add lyrics for every 2nd track
                        if (index % 2 == 0) {
                            val file = File(filesDir, "${track.id}.txt")
                            file.writeText(DEFAULT_LYRICS)
                        }
                    }
                }
        }
    }
}
