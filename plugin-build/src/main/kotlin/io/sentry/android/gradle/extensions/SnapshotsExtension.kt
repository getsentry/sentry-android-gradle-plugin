package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
open class SnapshotsExtension @Inject constructor(objects: ObjectFactory) {

  /** The application identifier used to associate snapshots with an app. */
  val appId: Property<String> = objects.property(String::class.java)

  /** The path to the folder containing snapshots to upload. */
  val path: DirectoryProperty = objects.directoryProperty()

  /** Enable automatic snapshot testing for @Preview composables. Defaults to false. */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  /** Include private @Preview functions in the scan. Defaults to false. */
  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** The build variant to scan for @Preview composables. Defaults to "debug". */
  val variant: Property<String> = objects.property(String::class.java).convention("debug")
}
