package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
open class SnapshotPreviewsExtension @Inject constructor(objects: ObjectFactory) {

  val generateTests: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)

  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)

  val theme: Property<String> = objects.property(String::class.java)
}
