package io.sentry.android.gradle.instrumentation.classloader.mapping

import io.sentry.android.gradle.instrumentation.classloader.Callable
import io.sentry.android.gradle.instrumentation.classloader.EntityDeletionOrUpdateAdapter
import io.sentry.android.gradle.instrumentation.classloader.SharedSQLiteStatement
import io.sentry.android.gradle.instrumentation.classloader.standardClassSource

val deletionDaoMissingClasses =
  arrayOf<Pair<String, (String) -> String>>(
    "io.sentry.android.roomsample.data.DeletionDao_Impl$1" to
      { name ->
        standardClassSource(name, EntityDeletionOrUpdateAdapter("Track"))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$2" to
      { name ->
        standardClassSource(name, EntityDeletionOrUpdateAdapter("MultiPKeyEntity"))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$3" to
      { name ->
        standardClassSource(name, EntityDeletionOrUpdateAdapter("Album"))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$4" to
      { name ->
        standardClassSource(name, SharedSQLiteStatement())
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$5" to
      { name ->
        standardClassSource(name, SharedSQLiteStatement())
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$6" to
      { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$7" to
      { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$8" to
      { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$9" to
      { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$10" to
      { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
      },
    "io.sentry.android.roomsample.data.DeletionDao_Impl$11" to
      { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
      },
  )
