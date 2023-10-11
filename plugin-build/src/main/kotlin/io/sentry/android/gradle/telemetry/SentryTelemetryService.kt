package io.sentry.android.gradle.telemetry

import io.sentry.HubAdapter
import io.sentry.IHub
import io.sentry.Sentry
import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input

abstract class SentryTelemetryService : BuildService<SentryTelemetryService.Params>,
    AutoCloseable {
    // Some parameters for the web server
    interface Params : BuildServiceParameters {
        @get:Input
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

    companion object {
        fun register(
            project: Project,
            dsn: String
        ): Provider<SentryTelemetryService> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryTelemetryService::class.java),
                SentryTelemetryService::class.java
            ) {
                it.parameters.dsn.set(dsn)
            }
        }
    }
}
