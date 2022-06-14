package io.sentry.android.gradle.util

import io.sentry.android.gradle.services.SentryModulesService
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider

fun Project.detectSentryAndroidSdk(
    configurationName: String,
    variantName: String,
    sentryModulesService: Provider<SentryModulesService>
) {
    val configProvider = try {
        configurations.named(configurationName)
    } catch (e: UnknownDomainObjectException) {
        logger.warn {
            "Unable to find configuration $configurationName for variant $variantName."
        }
        sentryModulesService.get().modules = emptyMap()
        return
    }

    configProvider.configure { configuration ->
        configuration.incoming.afterResolve {
            val sentryModules = it.resolutionResult.allComponents.filterSentryModules(logger)
            logger.info {
                "Detected Sentry modules $sentryModules " +
                    "for variant: $variantName, config: $configurationName"
            }
            sentryModulesService.get().modules = sentryModules
        }
    }
}

private fun Set<ResolvedComponentResult>.filterSentryModules(logger: Logger): Map<String, SemVer> {
    return filter { resolvedComponent: ResolvedComponentResult ->
        val moduleVersion = resolvedComponent.moduleVersion ?: return@filter false
        moduleVersion.group == "io.sentry"
    }.associate {
        val name = it.moduleVersion?.name ?: ""
        val version = it.moduleVersion?.version ?: ""
        val semver = try {
            SemVer.parse(it.moduleVersion?.version ?: "")
        } catch (e: Throwable) {
            logger.info { "Unable to parse version $version of $name" }
            SemVer()
        }
        name to semver
    }
}
