package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class SdkOptimizationExtension @Inject constructor(objects: ObjectFactory) {
  /** Enables build-time optimizations of the Sentry SDK. Defaults to true. */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
