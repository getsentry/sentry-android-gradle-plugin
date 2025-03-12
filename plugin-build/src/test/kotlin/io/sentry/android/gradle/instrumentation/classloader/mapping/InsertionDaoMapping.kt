package io.sentry.android.gradle.instrumentation.classloader.mapping

import io.sentry.android.gradle.instrumentation.classloader.EntityInsertionAdapter
import io.sentry.android.gradle.instrumentation.classloader.standardClassSource

val insertionDaoMissingClasses =
  arrayOf<Pair<String, (String) -> String>>(
    "io.sentry.android.roomsample.data.InsertionDao_Impl$1" to
      { name ->
        standardClassSource(name, EntityInsertionAdapter("Track"))
      },
    "io.sentry.android.roomsample.data.InsertionDao_Impl$2" to
      { name ->
        standardClassSource(name, EntityInsertionAdapter("Track"))
      },
    "io.sentry.android.roomsample.data.InsertionDao_Impl$3" to
      { name ->
        standardClassSource(name, EntityInsertionAdapter("Album"))
      },
  )
