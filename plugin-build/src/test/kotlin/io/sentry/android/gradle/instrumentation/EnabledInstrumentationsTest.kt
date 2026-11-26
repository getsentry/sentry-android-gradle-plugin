package io.sentry.android.gradle.instrumentation

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.SentryModuleVersions
import io.sentry.android.gradle.util.SentryVersions
import org.junit.Test

class EnabledInstrumentationsTest {

  private fun sut(
    features: Set<InstrumentationFeature> = emptySet(),
    versions: Map<String, String> = emptyMap(),
    logcatEnabled: Boolean = true,
    appStartEnabled: Boolean = false,
  ) =
    EnabledInstrumentations(
      SentryModuleVersions(versions),
      features,
      logcatEnabled,
      appStartEnabled,
    )

  private fun sqlite(version: SemVer) = mapOf(SENTRY_ANDROID_SQLITE to version.toString())

  @Test
  fun `sqliteDriver is on when sentry-android-sqlite meets the threshold and DATABASE is enabled`() {
    val enabled =
      sut(
        features = setOf(InstrumentationFeature.DATABASE),
        versions = sqlite(SentryVersions.VERSION_SQLITE_DRIVER),
      )

    assertThat(enabled.sqliteDriver).isTrue()
  }

  @Test
  fun `sqliteDriver is off when sentry-android-sqlite is absent from the classpath`() {
    val enabled = sut(features = setOf(InstrumentationFeature.DATABASE))

    assertThat(enabled.sqliteDriver).isFalse()
  }

  @Test
  fun `sqliteDriver is off one patch below VERSION_SQLITE_DRIVER`() {
    val belowThreshold =
      SemVer(
        SentryVersions.VERSION_SQLITE_DRIVER.major,
        SentryVersions.VERSION_SQLITE_DRIVER.minor - 1,
        99,
      )
    val enabled =
      sut(features = setOf(InstrumentationFeature.DATABASE), versions = sqlite(belowThreshold))

    assertThat(enabled.sqliteDriver).isFalse()
  }

  @Test
  fun `sqliteDriver is off one minor below VERSION_SQLITE_DRIVER`() {
    val belowThreshold =
      SemVer(
        SentryVersions.VERSION_SQLITE_DRIVER.major,
        SentryVersions.VERSION_SQLITE_DRIVER.minor - 1,
        SentryVersions.VERSION_SQLITE_DRIVER.patch,
      )
    val enabled =
      sut(features = setOf(InstrumentationFeature.DATABASE), versions = sqlite(belowThreshold))

    assertThat(enabled.sqliteDriver).isFalse()
  }

  @Test
  fun `sqliteDriver is off when DATABASE is disabled`() {
    val enabled = sut(versions = sqlite(SentryVersions.VERSION_SQLITE_DRIVER))

    assertThat(enabled.sqliteDriver).isFalse()
  }

  @Test
  fun `VERSION_SQLITE_DRIVER is greater than or equal to VERSION_SQLITE`() {
    // Gating relies on the presence of the open helper whenever the driver is present: both
    // instrumentables fire together and we rely on SentrySQLiteDriver.create to dedup the
    // SupportSQLiteDriver bridge case.
    assertThat(SentryVersions.VERSION_SQLITE_DRIVER >= SentryVersions.VERSION_SQLITE).isTrue()
  }

  @Test
  fun `between VERSION_SQLITE and VERSION_SQLITE_DRIVER only the open-helper path is on`() {
    val enabled =
      sut(
        features = setOf(InstrumentationFeature.DATABASE),
        versions =
          mapOf(
            SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE.toString(),
            SENTRY_ANDROID_CORE to SentryVersions.VERSION_PERFORMANCE.toString(),
          ),
      )

    assertThat(enabled.newDatabase).isTrue()
    assertThat(enabled.sqliteDriver).isFalse()
    assertThat(enabled.oldDatabase).isFalse()
  }

  @Test
  fun `at VERSION_SQLITE_DRIVER the open-helper path is also on and the old path is off`() {
    val enabled =
      sut(
        features = setOf(InstrumentationFeature.DATABASE),
        versions =
          mapOf(
            SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER.toString(),
            SENTRY_ANDROID_CORE to SentryVersions.VERSION_PERFORMANCE.toString(),
          ),
      )

    assertThat(enabled.sqliteDriver).isTrue()
    assertThat(enabled.newDatabase).isTrue() // superset relationship
    assertThat(enabled.oldDatabase).isFalse() // suppressed by !newDatabase
  }

  @Test
  fun `an unparseable version reads as the default version and gates everything off`() {
    val enabled =
      sut(
        features = setOf(InstrumentationFeature.DATABASE),
        versions = mapOf(SENTRY_ANDROID_SQLITE to "unspecified"),
      )

    assertThat(enabled.newDatabase).isFalse()
    assertThat(enabled.sqliteDriver).isFalse()
  }

  @Test
  fun `integrationNames covers the enabled features plus logcat and app start`() {
    val enabled =
      sut(
        features = setOf(InstrumentationFeature.DATABASE, InstrumentationFeature.FILE_IO),
        versions =
          mapOf(
            SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE.toString(),
            SENTRY_ANDROID_CORE to SentryVersions.VERSION_APP_START.toString(),
          ),
        appStartEnabled = true,
      )

    assertThat(enabled.integrationNames)
      .containsExactly(
        "DatabaseInstrumentation",
        "FileIOInstrumentation",
        "LogcatInstrumentation",
        "AppStartInstrumentation",
      )
  }

  @Test
  fun `integrationNames omits features whose module version is too old`() {
    val enabled =
      sut(
        features = setOf(InstrumentationFeature.DATABASE),
        versions = mapOf(SENTRY_ANDROID_CORE to "1.0.0"),
        logcatEnabled = false,
      )

    assertThat(enabled.integrationNames).isEmpty()
  }

  companion object {
    private const val SENTRY_ANDROID_CORE = "io.sentry:sentry-android-core"
    private const val SENTRY_ANDROID_SQLITE = "io.sentry:sentry-android-sqlite"
  }
}
