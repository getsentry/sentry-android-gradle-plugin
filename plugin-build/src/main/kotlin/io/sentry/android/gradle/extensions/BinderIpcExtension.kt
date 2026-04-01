package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class BinderIpcExtension @Inject constructor(objects: ObjectFactory) {
  /** Enables or disables Binder IPC call instrumentation. Defaults to true. */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
