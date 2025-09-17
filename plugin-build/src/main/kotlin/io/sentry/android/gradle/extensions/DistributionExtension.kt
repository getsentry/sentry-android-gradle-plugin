package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
open class DistributionExtension @Inject constructor(objects: ObjectFactory) {

  /**
   * Set of Android build variants that should have distribution enabled.
   *
   * Note: The global ignore settings (ignoredVariants, ignoredBuildTypes, ignoredFlavors)
   * have no relation to distribution and do not affect which variants are enabled here.
   */
  val enabledVariants: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())
}
