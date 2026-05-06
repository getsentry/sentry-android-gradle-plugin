package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.telemetry.SentryTelemetryService.Companion.SENTRY_SAAS_DSN
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Settings-level extension for shared Sentry configuration across all modules.
 *
 * Properties here are duplicated from [SentryPluginExtension] because that class requires a
 * [org.gradle.api.Project] in its constructor, which is not available during settings evaluation.
 * The project-level plugin bridges the two via [SentryPluginExtension.applySettingsDefaults],
 * which wires these values as conventions (overridable defaults) on each module's extension.
 */
abstract class SentrySettingsExtension @Inject constructor(objects: ObjectFactory) {

  /** The slug of the Sentry organization. Applied as the default for all modules. */
  val org: Property<String> = objects.property(String::class.java)

  /** The slug of the Sentry project. Applied as the default for all modules. */
  val projectName: Property<String> = objects.property(String::class.java)

  /**
   * The authentication token for uploading to Sentry. Applied as the default for all modules.
   *
   * WARNING: Do not hard-code this in settings.gradle — use an environment variable.
   */
  val authToken: Property<String> = objects.property(String::class.java)

  /** The URL of your Sentry instance. Only needed for self-hosted Sentry. */
  val url: Property<String> = objects.property(String::class.java)

  /** Enables debug log output for sentry-cli. Default is disabled. */
  val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  /** Whether the plugin should send telemetry data to Sentry. Default is enabled. */
  val telemetry: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

  /** The DSN telemetry data is sent to. Default is Sentry SAAS. */
  val telemetryDsn: Property<String> =
    objects.property(String::class.java).convention(SENTRY_SAAS_DSN)

  val autoInstallation: AutoInstallExtension = objects.newInstance(AutoInstallExtension::class.java)

  fun autoInstallation(autoInstallationAction: Action<AutoInstallExtension>) {
    autoInstallationAction.execute(autoInstallation)
  }

  /** Whether to include source context from all modules. Default is disabled. */
  val includeSourceContext: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)
}
