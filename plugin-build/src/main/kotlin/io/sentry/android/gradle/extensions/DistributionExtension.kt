package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.util.CiUtils.isCi
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
open class DistributionExtension
@Inject
constructor(objects: ObjectFactory, providerFactory: ProviderFactory) {

  /**
   * Controls whether build distribution uploads are enabled.
   *
   * Defaults to false.
   */
  val enabled: Property<Boolean> =
    objects.property(Boolean::class.java).convention(providerFactory.isCi() && false)

  /**
   * Set of Android build variants that should have the auto-update SDK added and auth token
   * embedded.
   *
   * This must be a subset of variants not in ignoredVariants. It is a build-time error to specify a
   * variant that is ignored by the Sentry plugin.
   *
   * Note: This controls auto-update SDK installation only. The [enabled] property controls whether
   * builds are uploaded for distribution.
   */
  val updateSdkVariants: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())

  /** Auth token used for distribution operations. */
  val authToken: Property<String> =
    objects.property(String::class.java).convention(System.getenv("SENTRY_DISTRIBUTION_AUTH_TOKEN"))
}
