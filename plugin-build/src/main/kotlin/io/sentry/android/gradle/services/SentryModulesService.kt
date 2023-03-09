@file:Suppress("UnstableApiUsage") // Shared build services are incubating but available from 6.1

package io.sentry.android.gradle.services

import com.android.build.gradle.internal.utils.setDisallowChanges
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.SentryVersions
import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input

abstract class SentryModulesService : BuildService<SentryModulesService.Parameters> {

    @get:Synchronized
    @set:Synchronized
    var sentryModules: Map<ModuleIdentifier, SemVer> = emptyMap()

    @get:Synchronized
    @set:Synchronized
    var externalModules: Map<ModuleIdentifier, SemVer> = emptyMap()

    fun retrieveEnabledInstrumentationFeatures(): Set<InstrumentationFeature> {
        return parameters.features.get().filter { isInstrumentationEnabled(it) }.toSet()
    }

    private fun isInstrumentationEnabled(feature: InstrumentationFeature): Boolean {
        return when (feature) {
            InstrumentationFeature.DATABASE -> isDatabaseInstrEnabled()
            InstrumentationFeature.FILE_IO -> isFileIOInstrEnabled()
            InstrumentationFeature.OKHTTP -> isOkHttpInstrEnabled()
            InstrumentationFeature.COMPOSE -> isComposeInstrEnabled()
        }
    }

    private fun isDatabaseInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_CORE,
            SentryVersions.VERSION_PERFORMANCE
        ) && parameters.features.get().contains(InstrumentationFeature.DATABASE)

    private fun isFileIOInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_CORE,
            SentryVersions.VERSION_FILE_IO
        ) && parameters.features.get().contains(InstrumentationFeature.FILE_IO)

    private fun isOkHttpInstrEnabled(): Boolean = sentryModules.isAtLeast(
        SentryModules.SENTRY_ANDROID_OKHTTP,
        SentryVersions.VERSION_OKHTTP
    ) && parameters.features.get().contains(InstrumentationFeature.OKHTTP)

    private fun isComposeInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_COMPOSE,
            SentryVersions.VERSION_COMPOSE
        ) && parameters.features.get().contains(InstrumentationFeature.COMPOSE)

    private fun Map<ModuleIdentifier, SemVer>.isAtLeast(
        module: ModuleIdentifier,
        minVersion: SemVer
    ): Boolean =
        getOrDefault(module, SentryVersions.VERSION_DEFAULT) >= minVersion

    companion object {
        fun register(
            project: Project,
            features: Provider<Set<InstrumentationFeature>>
        ): Provider<SentryModulesService> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryModulesService::class.java),
                SentryModulesService::class.java
            ) {
                it.parameters.features.setDisallowChanges(features)
            }
        }
    }

    interface Parameters : BuildServiceParameters {
        @get:Input
        val features: SetProperty<InstrumentationFeature>
    }
}
