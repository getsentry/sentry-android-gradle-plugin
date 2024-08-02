package io.sentry.android.gradle.instrumentation.classloader.mapping

import io.sentry.android.gradle.instrumentation.classloader.standardClassSource

val okHttpMissingClasses =
  arrayOf<Pair<String, (String) -> String>>(
    "okhttp3.internal.http.RealInterceptorChain" to
      { name ->
        standardClassSource(name, interfaces = arrayOf("okhttp3.Interceptor.Chain"))
      },
    "okhttp3.OkHttpClient\$1" to
      { name ->
        standardClassSource(name, superclass = "okhttp3.internal.Internal")
      },
  )
