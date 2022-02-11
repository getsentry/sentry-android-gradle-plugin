package io.sentry.android.gradle.util

import io.sentry.android.gradle.services.SentrySdkStateHolder
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider

fun Project.detectSentryAndroidSdk(
    configurationName: String,
    variantName: String,
    sdkStateHolder: Provider<SentrySdkStateHolder>
) {
    val configProvider = try {
        configurations.named(configurationName)
    } catch (e: UnknownDomainObjectException) {
        logger.warn {
            "Unable to find configuration $configurationName for variant $variantName."
        }
        sdkStateHolder.get().sdkState = SentryAndroidSdkState.MISSING
        return
    }

    configProvider.configure { configuration ->
        configuration.incoming.afterResolve {
            val version = it.resolutionResult.allComponents.findSentryAndroidSdk()
            if (version == null) {
                logger.warn { "sentry-android dependency was not found." }
                sdkStateHolder.get().sdkState = SentryAndroidSdkState.MISSING
                return@afterResolve
            }

            val state = try {
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
            sdkStateHolder.get().sdkState = state
        }
    }
}

private fun Set<ResolvedComponentResult>.findSentryAndroidSdk(): String? {
    val sentryDep = find { resolvedComponent: ResolvedComponentResult ->
        val moduleVersion = resolvedComponent.moduleVersion ?: return@find false
        moduleVersion.group == "io.sentry" && moduleVersion.name == "sentry-android-core"
    }
    return sentryDep?.moduleVersion?.version
}
