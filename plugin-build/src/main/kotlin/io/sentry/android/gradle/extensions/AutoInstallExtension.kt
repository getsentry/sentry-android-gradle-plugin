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
   * Fails the build when the OpenTelemetry versions resolved on the runtime classpath were
   * downgraded below what the Sentry OpenTelemetry integration requires (which leads to
   * ClassNotFoundException / NoSuchMethodError at runtime). Defaults to true.
   */
  val verifyOpenTelemetryVersions: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)
}
