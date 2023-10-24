package io.sentry.android.gradle.telemetry

import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.api.Task
import org.gradle.api.provider.Provider

/**
 * An ext function for tasks that wrap sentry-cli, which provides common error handling. Must be
 * called at configuration phase (=when registering a task).
 */
fun Task.withSentryTelemetry(extension: SentryPluginExtension, sentryTelemetryProvider: Provider<SentryTelemetryService>?) {
    sentryTelemetryProvider?.let { usesService(it) }
    var sentrySpan: ISpan? = null
    doFirst {
        if (extension.telemetry.orNull != false) {
            sentrySpan = sentryTelemetryProvider?.orNull?.startTask(this.javaClass.simpleName)
        }
    }

    doLast {
//        println(">>>>> doLast ${it.project.name} ${Sentry::class.java.classLoader}")
        println("### ${sentryTelemetryProvider?.orNull}")
        if (extension.telemetry.orNull != false) {
            sentryTelemetryProvider?.orNull?.endTask(sentrySpan, this)
        }
    }
}
