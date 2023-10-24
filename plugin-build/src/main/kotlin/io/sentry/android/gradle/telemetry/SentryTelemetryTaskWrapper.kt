package io.sentry.android.gradle.telemetry

import io.sentry.ISpan
import org.gradle.api.Task
import org.gradle.api.provider.Provider

/**
 * An ext function for tasks that wrap sentry-cli, which provides common error handling. Must be
 * called at configuration phase (=when registering a task).
 */
fun Task.withSentryTelemetry(sentryTelemetryProvider: Provider<SentryTelemetryService>?) {
    sentryTelemetryProvider?.let { usesService(it) }
    var sentrySpan: ISpan? = null
    doFirst {
        sentrySpan = sentryTelemetryProvider?.orNull?.startTask(this.javaClass.simpleName)
    }

    doLast {
        sentryTelemetryProvider?.orNull?.endTask(sentrySpan, this)
    }
}
