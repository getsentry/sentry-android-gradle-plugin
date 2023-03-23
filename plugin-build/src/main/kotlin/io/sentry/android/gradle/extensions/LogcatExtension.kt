package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class LogcatExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Enables or disables the Logcat feature.
     * Defaults to true.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * The minimum log level to capture.
     * Defaults to Level.WARNING.
     */
    val minLevel: Property<LogcatLevel> =
        objects.property(LogcatLevel::class.java).convention(LogcatLevel.WARNING)
}