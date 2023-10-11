package io.sentry.android.gradle.telemetry

import io.sentry.HubAdapter
import io.sentry.IHub
import io.sentry.Sentry
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class SentryTelemetryService : BuildService<SentryTelemetryService.Params>,
    AutoCloseable {
    // Some parameters for the web server
    internal interface Params : BuildServiceParameters {
        val dsn: Property<String>
    }

    private val hub: IHub

    init {
        Sentry.init(parameters.dsn.get())
        hub = HubAdapter.getInstance()

        // Start the server ...
//        println(java.lang.String.format("Server is running at %s", uri))
    }

    // A public method for tasks to use
    fun captureError(message: String) {
        println("capturing error $message")
        hub.captureMessage(message)
    }

    override fun close() {
        Sentry.close()
    }
}
