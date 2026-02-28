package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class SnapshotExtension @Inject constructor(objects: ObjectFactory) {

  /** Enable automatic snapshot testing for @Preview composables. Defaults to false. */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  /** Include private @Preview functions in the scan. Defaults to false. */
  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** The build variant to scan for @Preview composables. Defaults to "debug". */
  val variant: Property<String> = objects.property(String::class.java).convention("debug")
}
