package io.sentry.android.gradle.autoinstall

import org.gradle.api.artifacts.dsl.ComponentMetadataHandler

interface InstallStrategyRegistrar {
    fun register(component: ComponentMetadataHandler)
}
