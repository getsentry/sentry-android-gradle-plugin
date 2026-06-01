package io.sentry.android.gradle.snapshot.metadata

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

/**
 * Experimental extension for configuring @Preview metadata export. This API is subject to change
 * and will eventually be merged into the main `sentry` extension.
 */
@ApiStatus.Experimental
abstract class SentrySnapshotMetadataExtension(objects: ObjectFactory) {

  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)
}
