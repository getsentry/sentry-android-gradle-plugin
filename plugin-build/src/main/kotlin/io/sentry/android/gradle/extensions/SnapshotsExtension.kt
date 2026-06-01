package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
open class SnapshotsExtension @Inject constructor(objects: ObjectFactory) {

  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  val diffThreshold: Property<Double> = objects.property(Double::class.java).convention(0.0)

  val previews: SnapshotPreviewsExtension =
    objects.newInstance(SnapshotPreviewsExtension::class.java)

  fun previews(action: Action<SnapshotPreviewsExtension>) {
    action.execute(previews)
  }
}
