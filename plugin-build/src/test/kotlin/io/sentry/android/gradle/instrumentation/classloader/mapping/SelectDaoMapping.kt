package io.sentry.android.gradle.instrumentation.classloader.mapping

import io.sentry.android.gradle.instrumentation.classloader.Callable
import io.sentry.android.gradle.instrumentation.classloader.LimitOffsetPagingSource
import io.sentry.android.gradle.instrumentation.classloader.standardClassSource

val selectDaoMissingClasses = arrayOf<Pair<String, (String) -> String>>(
    "io.sentry.android.roomsample.data.SelectDao_Impl$1" to { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
    },
    "io.sentry.android.roomsample.data.SelectDao_Impl$2" to { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
    },
    "io.sentry.android.roomsample.data.SelectDao_Impl$3" to { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
    },
    "io.sentry.android.roomsample.data.SelectDao_Impl$4" to { name ->
        standardClassSource(name, interfaces = arrayOf(Callable()))
    },
    "io.sentry.android.roomsample.data.SelectDao_Impl$5" to { name ->
        standardClassSource(name, superclass = LimitOffsetPagingSource("SubAlbum"))
    }
)
