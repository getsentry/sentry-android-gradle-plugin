package io.sentry.android.gradle.util

import org.gradle.api.artifacts.ModuleIdentifier

/**
 * Resolved versions of the modules the plugin reasons about, keyed by `"group:name"`.
 *
 * The versions travel from dependency resolution to instrumentation as a plain string map so they
 * can be declared as a task/transform `@Input`: that makes them part of the build cache key, and
 * lets Gradle recompute them when it reuses a configuration cache entry.
 */
@JvmInline
value class SentryModuleVersions(val versions: Map<String, String>) {

  operator fun get(module: ModuleIdentifier): SemVer =
    versions[module.key()]?.let {
      try {
        SemVer.parse(it)
      } catch (e: Throwable) {
        SentryVersions.VERSION_DEFAULT
      }
    } ?: SentryVersions.VERSION_DEFAULT

  fun isAtLeast(module: ModuleIdentifier, minVersion: SemVer): Boolean = get(module) >= minVersion

  companion object {
    fun ModuleIdentifier.key(): String = "$group:$name"
  }
}
