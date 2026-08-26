package io.sentry.android.gradle.extensions

/**
 * Effective instrumentation flags after merging global defaults with reverse-DSL overrides.
 *
 * Precedence (first present wins):
 * 1. `sentry.variants.<variantName>`
 * 2. `sentry.buildTypes.<buildType>`
 * 3. `sentry.productFlavors.<flavor>` (earlier flavors win when multiple are set)
 * 4. global `sentry.tracingInstrumentation` / `sentry.runtimeOptimizations`
 */
data class EffectiveInstrumentationConfig(
  val tracingInstrumentationEnabled: Boolean,
  val runtimeOptimizationsEnabled: Boolean,
)

internal object SentryVariantConfigResolver {

  fun resolve(
    extension: SentryPluginExtension,
    variantName: String,
    buildType: String?,
    productFlavors: List<String>,
  ): EffectiveInstrumentationConfig {
    val byVariant = extension.variants.findByName(variantName)
    val byBuildType = buildType?.let { extension.buildTypes.findByName(it) }
    val byFlavors = productFlavors.mapNotNull { extension.productFlavors.findByName(it) }

    return EffectiveInstrumentationConfig(
      tracingInstrumentationEnabled =
        firstPresent(
          byVariant?.tracingInstrumentation?.enabled?.orNull,
          byBuildType?.tracingInstrumentation?.enabled?.orNull,
          *byFlavors.map { it.tracingInstrumentation.enabled.orNull }.toTypedArray(),
          extension.tracingInstrumentation.enabled.get(),
        ),
      runtimeOptimizationsEnabled =
        firstPresent(
          byVariant?.runtimeOptimizations?.enabled?.orNull,
          byBuildType?.runtimeOptimizations?.enabled?.orNull,
          *byFlavors.map { it.runtimeOptimizations.enabled.orNull }.toTypedArray(),
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
