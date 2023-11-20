package io.sentry.android.gradle.telemetry

import io.sentry.ISpan
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.nio.charset.Charset
import java.util.zip.CRC32
import org.gradle.api.Task
import org.gradle.api.provider.Provider

/**
 * An ext function for tasks that we want to collect telemetry for. Must be called at configuration
 * phase (=when registering a task).
 */
fun Task.withSentryTelemetry(
    extension: SentryPluginExtension,
    sentryTelemetryProvider: Provider<SentryTelemetryService>?
) {
    sentryTelemetryProvider?.let { usesService(it) }
    val projectHash = CRC32().also {
        it.update(this.project.name.toByteArray(Charset.defaultCharset()))
    }.value
    var sentrySpan: ISpan? = null
    doFirst {
        if (extension.telemetry.orNull != false) {
            sentrySpan = sentryTelemetryProvider?.orNull
                ?.startTask("${projectHash}_${this.javaClass.simpleName}")
        }
    }

    doLast {
        if (extension.telemetry.orNull != false) {
            sentryTelemetryProvider?.orNull?.endTask(sentrySpan, this)
        }
    }
}
