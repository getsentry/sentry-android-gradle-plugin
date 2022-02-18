package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import io.sentry.android.gradle.services.SentrySdkStateHolder
import io.sentry.android.gradle.util.getBuildServiceName
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class AutoInstallState : BuildService<BuildServiceParameters.None>, AutoCloseable, Serializable {
    @get:Synchronized
    @set:Synchronized
    var sentryVersion: String = SENTRY_SDK_VERSION

    @get:Synchronized
    @set:Synchronized
    var installOkHttp: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installFragment: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installTimber: Boolean = false

    override fun close() {
        println("close")
        sentryVersion = SENTRY_SDK_VERSION
        installTimber = false
        installFragment = false
        installOkHttp = false
    }

    companion object {
        fun register(project: Project): Provider<AutoInstallState> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(AutoInstallState::class.java),
                AutoInstallState::class.java
            ) {}
        }
    }
}
