package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.util.SentryModuleVersions
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.SentryVersions

/**
 * Which instrumentations apply, given the resolved Sentry versions and the user's configuration.
 *
 * The bytecode transform and the manifest integration list both answer their questions here, so a
 * manifest can never advertise an integration the bytecode doesn't have.
 */
class EnabledInstrumentations(
  private val versions: SentryModuleVersions,
  private val features: Set<InstrumentationFeature>,
  private val logcatEnabled: Boolean,
  private val appStartEnabled: Boolean,
) {

  val logcat: Boolean
    get() =
      logcatEnabled &&
        versions.isAtLeast(SentryModules.SENTRY_ANDROID_CORE, SentryVersions.VERSION_LOGCAT)

  val appStart: Boolean
    get() =
      appStartEnabled &&
        versions.isAtLeast(SentryModules.SENTRY_ANDROID_CORE, SentryVersions.VERSION_APP_START)

  val newDatabase: Boolean
    get() =
      InstrumentationFeature.DATABASE in features &&
        versions.isAtLeast(SentryModules.SENTRY_ANDROID_SQLITE, SentryVersions.VERSION_SQLITE)

  /**
   * We avoid gating on the Room version and rely on visitor behavior instead (no-ops for versions
   * of Room without a `SQLiteDriver`).
   */
  val sqliteDriver: Boolean
    get() =
      InstrumentationFeature.DATABASE in features &&
        versions.isAtLeast(
          SentryModules.SENTRY_ANDROID_SQLITE,
          SentryVersions.VERSION_SQLITE_DRIVER,
        )

  val oldDatabase: Boolean
    get() =
      !newDatabase &&
        InstrumentationFeature.DATABASE in features &&
        versions.isAtLeast(SentryModules.SENTRY_ANDROID_CORE, SentryVersions.VERSION_PERFORMANCE)

  val fileIO: Boolean
    get() =
      InstrumentationFeature.FILE_IO in features &&
        versions.isAtLeast(SentryModules.SENTRY_ANDROID_CORE, SentryVersions.VERSION_FILE_IO)

  val compose: Boolean
    get() =
      InstrumentationFeature.COMPOSE in features &&
        versions.isAtLeast(SentryModules.SENTRY_ANDROID_COMPOSE, SentryVersions.VERSION_COMPOSE)

  val okHttp: Boolean
    get() =
      InstrumentationFeature.OKHTTP in features &&
        (versions.isAtLeast(
          SentryModules.SENTRY_ANDROID_OKHTTP,
          SentryVersions.VERSION_ANDROID_OKHTTP,
        ) || versions.isAtLeast(SentryModules.SENTRY_OKHTTP, SentryVersions.VERSION_OKHTTP))

  val okHttpListener: Boolean
    get() =
      InstrumentationFeature.OKHTTP in features &&
        (versions.isAtLeast(
          SentryModules.SENTRY_ANDROID_OKHTTP,
          SentryVersions.VERSION_ANDROID_OKHTTP_LISTENER,
        ) || versions.isAtLeast(SentryModules.SENTRY_OKHTTP, SentryVersions.VERSION_OKHTTP))

  /** Names of the integrations written to the manifest's `io.sentry.gradle-plugin-integrations`. */
  val integrationNames: Set<String>
    get() = buildSet {
      features.forEach { feature ->
        if (feature.isEnabled()) {
          add(feature.integrationName)
        }
      }
      if (logcat) {
        add("LogcatInstrumentation")
      }
      if (appStart) {
        add("AppStartInstrumentation")
      }
    }

  private fun InstrumentationFeature.isEnabled(): Boolean =
    when (this) {
      InstrumentationFeature.DATABASE -> oldDatabase || newDatabase
      InstrumentationFeature.FILE_IO -> fileIO
      InstrumentationFeature.OKHTTP -> okHttp
      InstrumentationFeature.COMPOSE -> compose
    }
}
