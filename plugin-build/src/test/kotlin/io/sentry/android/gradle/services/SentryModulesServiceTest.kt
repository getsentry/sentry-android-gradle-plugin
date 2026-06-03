package io.sentry.android.gradle.services

import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.SentryVersions
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryModulesServiceTest {

  class Fixture {

    lateinit var sentryModulesServiceProvider: Provider<SentryModulesService>

    fun getSut(
      tmpDir: File,
      sentryModules: Map<ModuleIdentifier, SemVer> = emptyMap(),
      features: Set<InstrumentationFeature> = setOf(InstrumentationFeature.DATABASE),
    ): SentryModulesService {
      val project = ProjectBuilder.builder().withProjectDir(tmpDir).build()

      val featureProvider = project.provider { features }
      val logcatEnabled = project.provider { false }
      val sourceContextEnabled = project.provider { false }
      val dexguardEnabled = project.provider { false }
      val appStartEnabled = project.provider { false }

      sentryModulesServiceProvider =
        SentryModulesService.register(
          project,
          featureProvider,
          logcatEnabled,
          sourceContextEnabled,
          dexguardEnabled,
          appStartEnabled,
        )

      return sentryModulesServiceProvider.get().also { it.sentryModules = sentryModules }
    }
  }

  @get:Rule val testProjectDir = TemporaryFolder()

  private val fixture = Fixture()

  @Test
  fun `when sqlite version is above threshold and DATABASE feature is on - enabled`() {
    val sut =
      fixture.getSut(
        testProjectDir.root,
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SemVer(8, 44, 0)),
        features = setOf(InstrumentationFeature.DATABASE),
      )

    assertTrue { sut.isSQLiteDriverInstrEnabled() }
  }

  @Test
  fun `when sqlite version is below threshold - disabled`() {
    val sut =
      fixture.getSut(
        testProjectDir.root,
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SemVer(8, 42, 0)),
        features = setOf(InstrumentationFeature.DATABASE),
      )

    assertFalse { sut.isSQLiteDriverInstrEnabled() }
  }

  @Test
  fun `when sqlite module is absent - disabled`() {
    val sut =
      fixture.getSut(
        testProjectDir.root,
        sentryModules = emptyMap(),
        features = setOf(InstrumentationFeature.DATABASE),
      )

    assertFalse { sut.isSQLiteDriverInstrEnabled() }
  }

  @Test
  fun `when DATABASE feature is off but version qualifies - disabled`() {
    val sut =
      fixture.getSut(
        testProjectDir.root,
        sentryModules = mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SemVer(8, 44, 0)),
        features = emptySet(),
      )

    assertFalse { sut.isSQLiteDriverInstrEnabled() }
  }

  @Test
  fun `when sqlite version exactly equals threshold - enabled (locks gte semantics)`() {
    val sut =
      fixture.getSut(
        testProjectDir.root,
        sentryModules =
          mapOf(SentryModules.SENTRY_ANDROID_SQLITE to SentryVersions.VERSION_SENTRY_SQLITE_DRIVER),
        features = setOf(InstrumentationFeature.DATABASE),
      )

    assertTrue { sut.isSQLiteDriverInstrEnabled() }
  }
}
