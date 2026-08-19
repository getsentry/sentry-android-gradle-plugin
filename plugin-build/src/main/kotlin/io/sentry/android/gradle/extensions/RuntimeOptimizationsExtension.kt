package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

open class RuntimeOptimizationsExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enables runtime optimizations of the Sentry SDK at the cost of build time. Defaults to true.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

  /** Android build variants for which runtime optimizations should be disabled. */
  val ignoredVariants: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())

  /** Android build types for which runtime optimizations should be disabled. */
  val ignoredBuildTypes: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())

  /** Android build flavors for which runtime optimizations should be disabled. */
  val ignoredFlavors: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())
}
