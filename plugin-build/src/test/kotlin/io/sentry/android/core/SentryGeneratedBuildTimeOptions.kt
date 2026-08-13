package io.sentry.android.core

object SentryGeneratedBuildTimeOptions {
  var availability: Map<String, Boolean> = emptyMap()

  @JvmStatic fun getClassAvailability(): Map<String, Boolean> = availability
}
