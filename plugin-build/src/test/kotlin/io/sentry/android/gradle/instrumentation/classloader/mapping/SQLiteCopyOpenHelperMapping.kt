package io.sentry.android.gradle.instrumentation.classloader.mapping

import io.sentry.android.gradle.instrumentation.classloader.standardClassSource

val sqliteCopyOpenHelperMissingClasses =
  arrayOf<Pair<String, (String) -> String>>(
    "androidx.room.SQLiteCopyOpenHelper$1" to
      { name ->
        standardClassSource(name, "androidx.sqlite.db.SupportSQLiteOpenHelper.Callback")
      }
  )
