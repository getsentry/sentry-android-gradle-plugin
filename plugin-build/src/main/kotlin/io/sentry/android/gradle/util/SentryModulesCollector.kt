package io.sentry.android.gradle.util

import io.sentry.android.gradle.services.SentryModulesService
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider

fun Project.collectModules(
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
        sentryModulesService.get().sentryModules = emptyMap()
        sentryModulesService.get().externalModules = emptyMap()
        return
    }

    configProvider.configure { configuration ->
        configuration.incoming.afterResolve {
            val allModules = it.resolutionResult.allComponents.versionMap(logger)
            val sentryModules = allModules.filter { (identifier, _) ->
                identifier.group == "io.sentry"
            }.toMap()

            val externalModules = allModules.filter { (identifier, _) ->
                identifier.group != "io.sentry"
            }.toMap()

            logger.info {
                "Detected Sentry modules $sentryModules " +
                    "for variant: $variantName, config: $configurationName"
            }
            sentryModulesService.get().sentryModules = sentryModules
            sentryModulesService.get().externalModules = externalModules
        }
    }
}

private fun Set<ResolvedComponentResult>.versionMap(logger: Logger):
    List<Pair<ModuleIdentifier, SemVer>> {
    return mapNotNull {
        it.moduleVersion?.let { moduleVersion ->
            val identifier = moduleVersion.module
            val version = it.moduleVersion?.version ?: ""
            val semver = try {
                SemVer.parse(version)
            } catch (e: Throwable) {
                logger.info { "Unable to parse version $version of $identifier" }
                SemVer()
            }
            return@mapNotNull Pair(identifier, semver)
        }
        null
    }
}
