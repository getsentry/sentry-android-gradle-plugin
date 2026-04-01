package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
open class SnapshotsExtension @Inject constructor(objects: ObjectFactory) {

  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  val packageTrees: ListProperty<String> =
    objects.listProperty(String::class.java).convention(emptyList())

  val theme: Property<String> = objects.property(String::class.java)
}
