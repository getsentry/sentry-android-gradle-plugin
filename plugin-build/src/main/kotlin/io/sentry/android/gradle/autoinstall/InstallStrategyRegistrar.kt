package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler

interface InstallStrategyRegistrar {
    fun register(component: ComponentMetadataHandler)
}
