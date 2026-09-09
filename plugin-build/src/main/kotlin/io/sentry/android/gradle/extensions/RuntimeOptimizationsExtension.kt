package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class RuntimeOptimizationsExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enables runtime optimizations of the Sentry SDK at the cost of build time. Defaults to true.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
