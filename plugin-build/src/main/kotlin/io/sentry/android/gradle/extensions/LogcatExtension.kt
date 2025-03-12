package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class LogcatExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enables or disables the Logcat feature. When enabled and the Log call meets the minimum log
   * level, it will replace Logcat calls with SentryLogcatAdapter calls and add breadcrumbs.
   *
   * Defaults to true.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

  /** The minimum log level to capture. Defaults to Level.WARNING. */
  val minLevel: Property<LogcatLevel> =
    objects.property(LogcatLevel::class.java).convention(LogcatLevel.WARNING)
}
