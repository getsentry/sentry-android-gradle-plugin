package io.sentry.samples.instrumentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import io.sentry.samples.instrumentation.data.TracksDatabase

class SampleApp : Application() {

  companion object {
    lateinit var database: TracksDatabase
      private set

    lateinit var analytics: SharedPreferences
      private set
  }

  override fun onCreate() {
    super.onCreate()
    database =
      Room.databaseBuilder(this, TracksDatabase::class.java, "tracks.db")
        .createFromAsset("tracks.db")
        .fallbackToDestructiveMigration()
        .build()

    analytics = getSharedPreferences("analytics", Context.MODE_PRIVATE)
  }
}
