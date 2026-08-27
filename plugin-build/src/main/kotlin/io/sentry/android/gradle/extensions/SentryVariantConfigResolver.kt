package io.sentry.android.gradle.extensions

/**
 * Effective instrumentation flags after merging global defaults with reverse-DSL overrides.
 *
 * Precedence (first present wins):
 * 1. `sentry.variants.<variantName>`
 * 2. global `sentry.tracingInstrumentation` / `sentry.runtimeOptimizations`
 */
data class EffectiveInstrumentationConfig(
  val tracingInstrumentationEnabled: Boolean,
  val runtimeOptimizationsEnabled: Boolean,
)

internal object SentryVariantConfigResolver {

  fun resolve(
    extension: SentryPluginExtension,
    variantName: String,
  ): EffectiveInstrumentationConfig {
    val byVariant = extension.variants.findByName(variantName)

    return EffectiveInstrumentationConfig(
      tracingInstrumentationEnabled =
        firstPresent(
          byVariant?.tracingInstrumentation?.enabled?.orNull,
          extension.tracingInstrumentation.enabled.get(),
        ),
      runtimeOptimizationsEnabled =
        firstPresent(
          byVariant?.runtimeOptimizations?.enabled?.orNull,
          extension.runtimeOptimizations.enabled.get(),
        ),
    )
  }

  private fun firstPresent(vararg values: Boolean?): Boolean {
    for (value in values) {
      if (value != null) {
        return value
      }
    }
    error("Expected at least one non-null boolean when resolving instrumentation config")
  }
}
