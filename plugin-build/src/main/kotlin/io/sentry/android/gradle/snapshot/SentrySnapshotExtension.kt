package io.sentry.android.gradle.snapshot

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

/**
 * Experimental extension for configuring Compose @Preview snapshot testing. This API is subject to
 * change and will eventually be merged into the main `sentry` extension.
 */
@ApiStatus.Experimental
abstract class SentrySnapshotExtension(objects: ObjectFactory) {

  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  val packageTrees: ListProperty<String> =
    objects.listProperty(String::class.java).convention(emptyList())
}
