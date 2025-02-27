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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

abstract class SentryModulesService :
    BuildService<SentryModulesService.Parameters>,
    OperationCompletionListener {

    @get:Synchronized
    @set:Synchronized
    var sentryModules: Map<ModuleIdentifier, SemVer> = emptyMap()

    @get:Synchronized
    @set:Synchronized
    var externalModules: Map<ModuleIdentifier, SemVer> = emptyMap()

    fun retrieveEnabledInstrumentationFeatures(): Set<String> {
        val features = parameters.features.get()
            .filter { isInstrumentationEnabled(it) }
            .map { it.integrationName }
            .toMutableSet()

        if (isLogcatInstrEnabled()) {
            features.add("LogcatInstrumentation")
        }

        if (isAppStartInstrEnabled()) {
            features.add("AppStartInstrumentation")
        }

        if (parameters.sourceContextEnabled.getOrElse(false)) {
            features.add("SourceContext")
        }

        if (parameters.dexguardEnabled.getOrElse(false)) {
            features.add("DexGuard")
        }

        return features
    }

    private fun isInstrumentationEnabled(feature: InstrumentationFeature): Boolean {
        return when (feature) {
            InstrumentationFeature.DATABASE ->
                isOldDatabaseInstrEnabled() || isNewDatabaseInstrEnabled()
            InstrumentationFeature.FILE_IO -> isFileIOInstrEnabled()
            InstrumentationFeature.OKHTTP -> isOkHttpInstrEnabled()
            InstrumentationFeature.COMPOSE -> isComposeInstrEnabled()
        }
    }

    fun isLogcatInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_CORE,
            SentryVersions.VERSION_LOGCAT
        ) && parameters.logcatEnabled.get()

    fun isNewDatabaseInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_SQLITE,
            SentryVersions.VERSION_SQLITE
        ) && parameters.features.get().contains(InstrumentationFeature.DATABASE)

    fun isOldDatabaseInstrEnabled(): Boolean =
        !isNewDatabaseInstrEnabled() &&
            sentryModules.isAtLeast(
                SentryModules.SENTRY_ANDROID_CORE,
                SentryVersions.VERSION_PERFORMANCE
            ) && parameters.features.get().contains(InstrumentationFeature.DATABASE)

    fun isFileIOInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_CORE,
            SentryVersions.VERSION_FILE_IO
        ) && parameters.features.get().contains(InstrumentationFeature.FILE_IO)

    fun isOkHttpListenerInstrEnabled(): Boolean {
        val isSentryAndroidOkHttpListener = sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_OKHTTP,
            SentryVersions.VERSION_ANDROID_OKHTTP_LISTENER
        )
        val isSentryOkHttpListener = sentryModules.isAtLeast(
            SentryModules.SENTRY_OKHTTP,
            SentryVersions.VERSION_OKHTTP
        )
        return (isSentryAndroidOkHttpListener || isSentryOkHttpListener) &&
            parameters.features.get().contains(InstrumentationFeature.OKHTTP)
    }

    fun isOkHttpInstrEnabled(): Boolean {
        val isSentryAndroidOkHttp = sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_OKHTTP,
            SentryVersions.VERSION_ANDROID_OKHTTP
        )
        val isSentryOkHttp = sentryModules.isAtLeast(
            SentryModules.SENTRY_OKHTTP,
            SentryVersions.VERSION_OKHTTP
        )
        return (isSentryAndroidOkHttp || isSentryOkHttp) &&
            parameters.features.get().contains(InstrumentationFeature.OKHTTP)
    }

    fun isComposeInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_COMPOSE,
            SentryVersions.VERSION_COMPOSE
        ) && parameters.features.get().contains(InstrumentationFeature.COMPOSE)

    fun isAppStartInstrEnabled(): Boolean =
        sentryModules.isAtLeast(
            SentryModules.SENTRY_ANDROID_CORE,
            SentryVersions.VERSION_APP_START
        ) && parameters.appStartEnabled.get()

    private fun Map<ModuleIdentifier, SemVer>.isAtLeast(
        module: ModuleIdentifier,
        minVersion: SemVer
    ): Boolean =
        getOrDefault(module, SentryVersions.VERSION_DEFAULT) >= minVersion

    companion object {
        fun register(
            project: Project,
            features: Provider<Set<InstrumentationFeature>>,
            logcatEnabled: Provider<Boolean>,
            sourceContextEnabled: Provider<Boolean>,
            dexguardEnabled: Provider<Boolean>,
            appStartEnabled: Provider<Boolean>
        ): Provider<SentryModulesService> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryModulesService::class.java),
                SentryModulesService::class.java
            ) {
                it.parameters.features.setDisallowChanges(features)
                it.parameters.logcatEnabled.setDisallowChanges(logcatEnabled)
                it.parameters.sourceContextEnabled.setDisallowChanges(sourceContextEnabled)
                it.parameters.dexguardEnabled.setDisallowChanges(dexguardEnabled)
                it.parameters.appStartEnabled.setDisallowChanges(appStartEnabled)
            }
        }
    }

    override fun onFinish(event: FinishEvent?) = Unit // no-op

    interface Parameters : BuildServiceParameters {

        @get:Input
        val features: SetProperty<InstrumentationFeature>

        @get:Input
        val logcatEnabled: Property<Boolean>

        @get:Input
        val sourceContextEnabled: Property<Boolean>

        @get:Input
        val dexguardEnabled: Property<Boolean>

        @get:Input
        val appStartEnabled: Property<Boolean>
    }
}
