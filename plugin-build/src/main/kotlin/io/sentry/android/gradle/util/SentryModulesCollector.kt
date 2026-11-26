package io.sentry.android.gradle.util

import io.sentry.android.gradle.util.SentryModuleVersions.Companion.key
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Provider

/**
 * Resolved versions of the modules the instrumentation reasons about, as a provider over the
 * configuration's resolution result.
 *
 * Reading them through a provider rather than an `afterResolve` hook is what makes instrumentation
 * survive a reused configuration cache entry: the hook only runs on builds that configure, while
 * Gradle re-evaluates the provider on every build.
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

  return configProvider
    .flatMap { it.incoming.resolutionResult.rootComponent }
    .map { it.relevantModuleVersions() }
}

/**
 * Only the modules the instrumentation actually branches on, so that bumping an unrelated
 * dependency doesn't invalidate every instrumented class.
 */
private fun String.isRelevantModule(): Boolean =
  startsWith("io.sentry:") ||
    this == ExternalModules.ANDROIDX_SQLITE_FRAMEWORK.key() ||
    this == ExternalModules.OKHTTP.key()

private fun ResolvedComponentResult.relevantModuleVersions(): Map<String, String> {
  val versions = sortedMapOf<String, String>()
  val visited = mutableSetOf<ComponentIdentifier>()
  val queue = ArrayDeque<ResolvedComponentResult>()
  queue.add(this)

  while (queue.isNotEmpty()) {
    val component = queue.removeFirst()
    if (!visited.add(component.id)) {
      continue
    }
    component.moduleVersion?.let { moduleVersion ->
      val key = moduleVersion.module.key()
      if (key.isRelevantModule()) {
        versions[key] = moduleVersion.version
      }
    }
    component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach {
      queue.add(it.selected)
    }
  }
  return versions
}
