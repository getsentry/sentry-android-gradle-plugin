package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class BinderExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enables or disables Binder IPC call instrumentation. Defaults to true. This requires
   * sentry-android-core version TODO or above, and needs to be enabled. See
   * https://docs.sentry.io/platforms/android/configuration/gradle/#tracing-auto-instrumentation for
   * more details.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
