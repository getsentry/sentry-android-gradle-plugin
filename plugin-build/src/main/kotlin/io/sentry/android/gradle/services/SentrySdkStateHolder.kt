@file:Suppress("UnstableApiUsage") // Shared build services are incubating but available from 6.1

package io.sentry.android.gradle.services

import io.sentry.android.gradle.util.SentryAndroidSdkState
import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class SentrySdkStateHolder : BuildService<BuildServiceParameters.None> {

    @get:Synchronized
    @set:Synchronized
    var sdkState: SentryAndroidSdkState = SentryAndroidSdkState.MISSING

    companion object {
        fun register(project: Project): Provider<SentrySdkStateHolder> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentrySdkStateHolder::class.java),
                SentrySdkStateHolder::class.java
            ) {}
        }
    }
}
