package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Per-variant / build-type / product-flavor overrides for the Sentry plugin.
 *
 * Unset properties mean "inherit" from a more specific scope or the global extension. Do not set
 * conventions on override properties — only the global extension owns defaults.
 *
 * Example:
 * ```
 * sentry {
 *   buildTypes {
 *     debug {
 *       tracingInstrumentation { enabled = false }
 *       runtimeOptimizations { enabled = false }
 *     }
 *   }
 * }
 * ```
 */
open class SentryVariantConfig
@Inject
constructor(private val name: String, objects: ObjectFactory) : Named {

  override fun getName(): String = name

  @get:Nested
  val tracingInstrumentation: TracingInstrumentationOverride =
    objects.newInstance(TracingInstrumentationOverride::class.java)

  fun tracingInstrumentation(action: Action<TracingInstrumentationOverride>) {
    action.execute(tracingInstrumentation)
  }

  @get:Nested
  val runtimeOptimizations: RuntimeOptimizationsOverride =
    objects.newInstance(RuntimeOptimizationsOverride::class.java)

  fun runtimeOptimizations(action: Action<RuntimeOptimizationsOverride>) {
    action.execute(runtimeOptimizations)
  }
}

/** Variant-scoped override for [TracingInstrumentationExtension]. Unset = inherit. */
open class TracingInstrumentationOverride @Inject constructor(objects: ObjectFactory) {
  val enabled: Property<Boolean> = objects.property(Boolean::class.java)
}

/** Variant-scoped override for [RuntimeOptimizationsExtension]. Unset = inherit. */
open class RuntimeOptimizationsOverride @Inject constructor(objects: ObjectFactory) {
  val enabled: Property<Boolean> = objects.property(Boolean::class.java)
}
