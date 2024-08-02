package io.sentry.android.gradle.instrumentation.logcat

enum class LogcatLevel(private val level: Int) {
  VERBOSE(0),
  DEBUG(1),
  INFO(2),
  WARNING(3),
  ERROR(4);

  fun supports(other: LogcatLevel): Boolean {
    return level >= other.level
  }

  companion object {
    fun logFunctionToLevel(str: String): LogcatLevel? {
      return when (str) {
        "v" -> VERBOSE
        "d" -> DEBUG
        "i" -> INFO
        "w" -> WARNING
        "e" -> ERROR
        "wtf" -> ERROR
        else -> null
      }
    }
  }
}
