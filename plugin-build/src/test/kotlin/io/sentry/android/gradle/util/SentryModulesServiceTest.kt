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

    data class Sut(val service: SentryModulesService, val project: org.gradle.api.Project)

    fun getSut(
      tmpDir: File,
      features: Set<InstrumentationFeature> = emptySet(),
      sentryModules: Map<ModuleIdentifier, SemVer> = emptyMap(),
    ): Sut {
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
      return Sut(service, fakeProject)
    }
  }

  @get:Rule val testProjectDir = TemporaryFolder()

  private val fixture = Fixture()

  @Test
  fun `isSQLiteDriverInstrEnabled is true when sqlite version meets threshold and DATABASE feature is enabled`() {
    val (service, _) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules =
          mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER),
      )

    assertTrue(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when sqlite version is below threshold`() {
    val (service, _) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when DATABASE feature is not enabled`() {
    val (service, _) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = emptySet(),
        sentryModules =
          mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false when sentry-android-sqlite is not on the classpath`() {
    val (service, _) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = emptyMap(),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is true at exact VERSION_SQLITE_DRIVER threshold`() {
    val (service, _) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules =
          mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER),
      )

    assertTrue(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `isSQLiteDriverInstrEnabled is false one minor below VERSION_SQLITE_DRIVER threshold`() {
    val belowThreshold =
      SemVer(
        SentryVersions.VERSION_SQLITE_DRIVER.major,
        SentryVersions.VERSION_SQLITE_DRIVER.minor - 1,
        SentryVersions.VERSION_SQLITE_DRIVER.patch,
      )
    val (service, _) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to belowThreshold),
      )

    assertFalse(service.isSQLiteDriverInstrEnabled())
  }

  @Test
  fun `retrieveEnabledInstrumentationFeatures includes SQLiteDriver when gate passes`() {
    val (service, project) =
      fixture.getSut(
        tmpDir = testProjectDir.root,
        features = setOf(InstrumentationFeature.DATABASE),
        sentryModules =
          mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SQLITE_DRIVER),
      )

    val features = service.retrieveEnabledInstrumentationFeatures(project).get()

    assertTrue("SQLiteDriver" in features)
    assertTrue(InstrumentationFeature.DATABASE.integrationName in features)
  }
}
