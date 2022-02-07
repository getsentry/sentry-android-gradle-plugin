package io.sentry.android.gradle.util

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult

fun Project.getSentryAndroidSdkState(
    configurationName: String,
    variantName: String
): SentryAndroidSdkState {
    val configuration = configurations.findByName(configurationName)

    if (configuration == null) {
        logger.warn {
            "Unable to find configuration $configurationName for variant $variantName."
        }
        return SentryAndroidSdkState.MISSING
    }

    val dependencyResolution = configuration.incoming.resolutionResult

    val version = dependencyResolution.allComponents.findSentryAndroidSdk()
    if (version == null) {
        logger.warn { "sentry-android dependency was not found." }
        return SentryAndroidSdkState.MISSING
    }

    return try {
        val sdkState = SentryAndroidSdkState.from(version)
        logger.info {
            "Detected sentry-android $sdkState for version: $version, " +
                "variant: $variantName, config: $configurationName"
        }
        sdkState
    } catch (e: IllegalStateException) {
        logger.warn { e.localizedMessage }
        SentryAndroidSdkState.MISSING
    }
}

private fun Set<ResolvedComponentResult>.findSentryAndroidSdk(): String? {
    val sentryDep = find { resolvedComponent: ResolvedComponentResult ->
        val moduleVersion = resolvedComponent.moduleVersion ?: return@find false
        moduleVersion.group == "io.sentry" && moduleVersion.name == "sentry-android-core"
    }
    return sentryDep?.moduleVersion?.version
}
