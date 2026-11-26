package io.sentry.android.gradle.util

import io.sentry.android.gradle.services.SentryModulesService
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.provider.Provider

/**
 * Returns the versions of all modules on [configurationName], keyed by `group:name`.
 *
 * It is a lazy provider for [SentryModulesService] parameters, so that the configuration cache
 * stores the versions. A resolution callback does not run again when the cache is reused.
 */
fun Project.collectModules(
  configurationName: String,
  variantName: String,
): Provider<Map<String, String>> {
  val configProvider =
    try {
      configurations.named(configurationName)
    } catch (e: UnknownDomainObjectException) {
      logger.warn { "Unable to find configuration $configurationName for variant $variantName." }
      return provider { emptyMap() }
    }

  val logger = logger
  return configProvider.map { configuration ->
    val modules =
      configuration.incoming.resolutionResult.allComponents
        .mapNotNull { it.moduleVersion }
        .associate { "${it.group}:${it.name}" to it.version }
    logger.info {
      "Detected Sentry modules ${modules.filterKeys { it.startsWith("${SentryModules.SENTRY.group}:") }} " +
        "for variant: $variantName, config: $configurationName"
    }
    modules
  }
}
