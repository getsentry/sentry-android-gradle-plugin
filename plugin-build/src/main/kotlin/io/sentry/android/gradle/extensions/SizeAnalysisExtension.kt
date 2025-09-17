package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.util.CiUtils.isCi
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
open class SizeAnalysisExtension
@Inject
constructor(objects: ObjectFactory, providerFactory: ProviderFactory) {

  val enabled: Property<Boolean> =
    objects
      .property(Boolean::class.java)
      .convention(providerFactory.isCi() && false) // set to false for now otherwise upload fails CI

  /**
   * The build configuration to use for the upload. This allows comparison between builds with the
   * same buildConfiguration. If not provided, the build variant will be used.
   */
  val buildConfiguration: Property<String> = objects.property(String::class.java)
}
