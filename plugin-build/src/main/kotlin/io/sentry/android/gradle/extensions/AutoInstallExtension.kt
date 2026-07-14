package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class AutoInstallExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enable auto-installation of Sentry components (sentry-android SDK and okhttp, timber, fragment
   * and sqlite integrations). Defaults to true.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

  /**
   * Overrides default (bundled with plugin) or inherited (from user's buildscript) sentry version.
   * Defaults to the latest published sentry version.
   */
  val sentryVersion: Property<String> =
    objects.property(String::class.java).convention(SENTRY_SDK_VERSION)

  /**
   * Enable auto-installation of the sentry-async-profiler integration for JVM projects. Defaults to
   * false.
   *
   * When enabled, sentry-async-profiler is added to the project dependencies with the same version
   * as the Sentry SDK. Requires Sentry SDK version 8.23.0 or higher. Android projects do not
   * install this JVM-only dependency and emit a warning instead.
   */
  val installProfiler: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}
