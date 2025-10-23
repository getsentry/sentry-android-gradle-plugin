package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.util.CiUtils.isCi
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
open class SizeAnalysisExtension
@Inject
constructor(objects: ObjectFactory, providerFactory: ProviderFactory) {

  val enabled: Property<Boolean> =
    objects
      .property(Boolean::class.java)
      .convention(providerFactory.isCi() && false) // set to false for now otherwise upload fails CI

  /**
   * Set of Android build variants that should have size analysis enabled.
   *
   * When empty (default), size analysis runs on all variants (subject to the enabled flag).
   * When populated, only the specified variants will have size analysis enabled.
   *
   * Note: This property BYPASSES the global ignore settings (ignoredVariants, ignoredBuildTypes,
   * ignoredFlavors). This allows size analysis to run on specific variants even if they are
   * globally ignored. This is different from most other Sentry features which respect the global
   * ignore settings.
   *
   * Example:
   * ```
   * sentry {
   *   ignoredVariants.set(["debug"]) // Most features ignore debug
   *   sizeAnalysis {
   *     enabled = true
   *     enabledVariants.set(["debug", "release"]) // Size analysis runs on both
   *   }
   * }
   * ```
   */
  val enabledVariants: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())

  /**
   * The build configuration to use for the upload. This allows comparison between builds with the
   * same buildConfiguration. If not provided, the build variant will be used.
   */
  val buildConfiguration: Property<String> = objects.property(String::class.java)
}
