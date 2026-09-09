package io.sentry.android.core

object SentryGeneratedBuildTimeOptions {
  var availability: Map<String, Boolean> = emptyMap()
  var metadata: Map<String, Any>? = null

  @JvmStatic fun getClassAvailability(): Map<String, Boolean> = availability

  @JvmStatic fun getManifestMetadata(): Map<String, Any>? = metadata
}
