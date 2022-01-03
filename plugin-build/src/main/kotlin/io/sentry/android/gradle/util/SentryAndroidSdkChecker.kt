package io.sentry.android.gradle.util

import java.util.LinkedList
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

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

    val resolvedConfiguration = configuration.resolvedConfiguration
    if (resolvedConfiguration.hasError()) {
        resolvedConfiguration.rethrowFailure()
        logger.warn { "Unable to resolve configuration $configurationName." }
        return SentryAndroidSdkState.MISSING
    }

    val deps = resolvedConfiguration.firstLevelModuleDependencies
    val version = deps.findSentryAndroidSdk()
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

private fun Set<ResolvedDependency>.findSentryAndroidSdk(): String? {
    val queue = LinkedList(this)
    while (queue.isNotEmpty()) {
        val dep = queue.remove()
        if (dep.moduleGroup == "io.sentry" && dep.moduleName == "sentry-android") {
            return dep.moduleVersion
        }
        queue.addAll(dep.children)
    }
    return null
}
