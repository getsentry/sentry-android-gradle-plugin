package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
open class DistributionExtension @Inject constructor(objects: ObjectFactory) {

  /** Set of Android build variants that should have build distribution enabled. */
  val enabledFor: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())
}
