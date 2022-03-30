package io.sentry.android.gradle.instrumentation.classloader.mapping

import io.sentry.android.gradle.instrumentation.classloader.standardClassSource

val gmsMapping = arrayOf<Pair<String, (String) -> String>>(
    "com.google.android.gms.internal.measurement.zzhz" to { name ->
        standardClassSource(
            name,
            interfaces = arrayOf("android.app.Application.ActivityLifecycleCallbacks")
        )
    },
    "com.google.android.gms.internal.measurement.zzhi" to { name ->
        standardClassSource(
            name,
            interfaces = arrayOf("java.lang.Runnable")
        )
    }
)
