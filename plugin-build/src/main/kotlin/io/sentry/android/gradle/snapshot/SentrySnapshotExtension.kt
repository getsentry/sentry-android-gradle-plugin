package io.sentry.android.gradle.snapshot

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class SentrySnapshotExtension(objects: ObjectFactory) {

  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  val packageTrees: ListProperty<String> =
    objects.listProperty(String::class.java).convention(emptyList())
}
