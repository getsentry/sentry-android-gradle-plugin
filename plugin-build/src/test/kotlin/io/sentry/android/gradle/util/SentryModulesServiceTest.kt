package io.sentry.android.gradle.util

import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.services.SentryModulesService
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryModulesServiceTest {

  class Fixture {

    fun getSut(
      tmpDir: File,
      features: Set<InstrumentationFeature> = emptySet(),
      sentryModules: Map<ModuleIdentifier, SemVer> = emptyMap(),
      externalModules: Map<ModuleIdentifier, SemVer> = emptyMap(),
    ): SentryModulesService {
      val fakeProject = ProjectBuilder.builder().withProjectDir(tmpDir).build()

      val featureProvider = fakeProject.provider { features }
      val logcatEnabled = fakeProject.provider { true }
      val sourceContextEnabled = fakeProject.provider { false }
      val dexguardEnabled = fakeProject.provider { false }
      val appStartEnabled = fakeProject.provider { false }

      val serviceProvider =
        SentryModulesService.register(
          fakeProject,
          featureProvider,
          logcatEnabled,
          sourceContextEnabled,
          dexguardEnabled,
          appStartEnabled,
        )
      val service = serviceProvider.get()
      service.sentryModules = sentryModules
      service.externalModules = externalModules
      return service
    }
  }

  @get:Rule val testProjectDir = TemporaryFolder()

  private val fixture = Fixture()

  private fun sqliteDriverSentryModules(): Map<ModuleIdentifier, SemVer> =
    mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER)

  @Test
  fun `isSQLiteDriverInstrEnabled is true when sentry-android-sqlite meets threshold and DATABASE is enabled`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = sqliteDriverSentryModules(),
      )

    assertTrue(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when sentry-android-sqlite is absent from classpath`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = emptyMap(),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when sentry-android-sqlite is just below VERSION_SQLITE_DRIVER`() {
    val belowThreshold =
      SemVer(
        SentryVersions.VERSION_SQLITE_DRIVER.major,
        SentryVersions.VERSION_SQLITE_DRIVER.minor - 1,
        99,
      )
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to belowThreshold),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when sentry-android-sqlite is one minor below VERSION_SQLITE_DRIVER`() {
    val belowThreshold =
      SemVer(
        SentryVersions.VERSION_SQLITE_DRIVER.major,
        SentryVersions.VERSION_SQLITE_DRIVER.minor - 1,
        SentryVersions.VERSION_SQLITE_DRIVER.patch,
      )
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to belowThreshold),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when DATABASE is disabled`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = emptySet(),
        sentryModules = sqliteDriverSentryModules(),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `VERSION_SQLITE_DRIVER is greater than or equal to VERSION_SQLITE`() {
    // Gating relies on the presence of the open helper whenever the driver is present: both
    // instrumentables fire together and we rely on SentrySQLiteDriver.create to dedup the
    // SupportSQLiteDriver bridge case.
    assertTrue(SentryVersions.VERSION_SQLITE_DRIVER >= SentryVersions.VERSION_SQLITE)
  }

  @Test
  fun `between VERSION_SQLITE and VERSION_SQLITE_DRIVER only the open-helper path is on`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules =
          mapOf(
            SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE,
            SentryModules.SENTRY_ANDROID_CORE to SentryVersions.VERSION_PERFORMANCE,
          ),
      )

    assertTrue(service.isNewDatabaseInstrEnabled())
    assertFalse(service.isSQLiteDriverInstrEnabled())
    assertFalse(service.isOldDatabaseInstrEnabled())
  }

  @Test
  fun `at VERSION_SQLITE_DRIVER the open-helper path is also on and the old path is off`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules =
          mapOf(
            SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER,
            SentryModules.SENTRY_ANDROID_CORE to SentryVersions.VERSION_PERFORMANCE,
          ),
      )

    assertTrue(service.isSQLiteDriverInstrEnabled())
    assertTrue(service.isNewDatabaseInstrEnabled()) // superset relationship
    assertFalse(service.isOldDatabaseInstrEnabled()) // suppressed by !isNewDatabaseInstrEnabled()
  }

  private fun navigation3SentryModules(
    version: SemVer = SentryVersions.VERSION_NAVIGATION3
  ): Map<ModuleIdentifier, SemVer> = mapOf(SentryModules.SENTRY_ANDROID_NAVIGATION3 to version)

  private fun navigation3ExternalModules(
    version: SemVer = SemVer(1, 0, 0),
    module: ModuleIdentifier = ExternalModules.NAVIGATION3_UI,
  ): Map<ModuleIdentifier, SemVer> = mapOf(module to version)

  @Test
  fun `isNavigation3InstrEnabled is true when all requirements are met`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.NAVIGATION3),
        sentryModules = navigation3SentryModules(),
        externalModules = navigation3ExternalModules(),
      )

    assertTrue(service.isNavigation3InstrEnabled())
  }

  @Test
  fun `isNavigation3InstrEnabled accepts the navigation3-ui-android artifact`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.NAVIGATION3),
        sentryModules = navigation3SentryModules(),
        externalModules =
          navigation3ExternalModules(
            version = SemVer(1, 2, 0),
            module = ExternalModules.NAVIGATION3_UI_ANDROID,
          ),
      )

    assertTrue(service.isNavigation3InstrEnabled())
  }

  @Test
  fun `isNavigation3InstrEnabled is false when NAVIGATION3 is disabled`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.COMPOSE),
        sentryModules = navigation3SentryModules(),
        externalModules = navigation3ExternalModules(),
      )

    assertFalse(service.isNavigation3InstrEnabled())
  }

  @Test
  fun `isNavigation3InstrEnabled is false when sentry-android-navigation3 is absent`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.NAVIGATION3),
        externalModules = navigation3ExternalModules(),
      )

    assertFalse(service.isNavigation3InstrEnabled())
  }

  @Test
  fun `isNavigation3InstrEnabled is false when sentry-android-navigation3 predates the entry point`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.NAVIGATION3),
        sentryModules = navigation3SentryModules(SemVer(8, 61, 1)),
        externalModules = navigation3ExternalModules(),
      )

    assertFalse(service.isNavigation3InstrEnabled())
  }

  @Test
  fun `isNavigation3InstrEnabled is false when Navigation 3 is absent`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.NAVIGATION3),
        sentryModules = navigation3SentryModules(),
      )

    assertFalse(service.isNavigation3InstrEnabled())
  }

  @Test
  fun `isNavigation3InstrEnabled is false for Navigation 3 pre-releases before 1_0_0`() {
    val service =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.NAVIGATION3),
        sentryModules = navigation3SentryModules(),
        externalModules = navigation3ExternalModules(SemVer.parse("1.0.0-rc01")),
      )

    assertFalse(service.isNavigation3InstrEnabled())
  }
}
