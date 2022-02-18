package io.sentry.android.gradle.autoinstall

import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.provider.Provider

interface InstallStrategyRegistrar {
    fun register(component: ComponentMetadataHandler, autoInstallState: Provider<AutoInstallState>)
}
