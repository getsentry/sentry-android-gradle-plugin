package io.sentry.android.gradle.util

import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.services.SentryModulesService
import java.io.File
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

class SentryModulesCollectorTest {

  class Fixture {

    private val configuration = mock<Configuration>()
    val resolvableDependencies = mock<ResolvableDependencies>()
    private val resolutionResult = mock<ResolutionResult>()

    val configurationName: String = "debugRuntimeClasspath"
    val variantName: String = "debug"

    private lateinit var sentryModulesServiceProvider: Provider<SentryModulesService>
    val logger = CapturingTestLogger()

    fun getSut(tmpDir: File, dependencies: Set<ResolvedComponentResult> = emptySet()): Project {
      whenever(configuration.name).thenReturn(configurationName)
      whenever(configuration.incoming).thenReturn(resolvableDependencies)
      whenever(resolvableDependencies.resolutionResult).thenReturn(resolutionResult)
      whenever(resolutionResult.allComponents).thenReturn(dependencies)
      val project = spy(ProjectBuilder.builder().withProjectDir(tmpDir).build())
      whenever(project.logger).thenReturn(logger)
      project.configurations.add(configuration)
      return project
    }

    fun collect(project: Project, configurationName: String, variantName: String) {
      sentryModulesServiceProvider =
        SentryModulesService.register(
          project,
          variantName,
          project.collectModules(configurationName, variantName),
          project.provider { emptySet<InstrumentationFeature>() },
          project.provider { true },
          project.provider { true },
          project.provider { true },
          project.provider { true },
        )
    }

    fun getSentryModules() = sentryModulesServiceProvider.get().sentryModules

    fun getExternalModules() = sentryModulesServiceProvider.get().externalModules
  }

  @get:Rule val testProjectDir = TemporaryFolder()

  private val fixture = Fixture()

  @Test
  fun `configuration cannot be found - logs a warning and does not provide modules`() {
    val project = fixture.getSut(testProjectDir.root)
    fixture.collect(project, "releaseRuntimeClasspath", "release")
    assertTrue { fixture.getSentryModules().isEmpty() }
    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] Unable to find configuration releaseRuntimeClasspath for variant release."
    }
  }

  @Test
  fun `sentry-android is not in the dependencies - modules list is empty`() {
    val sqliteDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(DefaultModuleVersionIdentifier.newId("androidx.sqlite", "sqlite", "2.1.0"))
      }

    val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sqliteDep))
    fixture.collect(project, fixture.configurationName, fixture.variantName)

    assertTrue { fixture.getSentryModules().isEmpty() }
  }

  @Test
  fun `sentry-android as a local dependency - module's version is omitted`() {
    val sentryAndroidDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(
            // this is the case when sentry-android is a local dependency
            DefaultModuleVersionIdentifier.newId("io.sentry", "sentry-android-core", "unspecified")
          )
      }

    val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sentryAndroidDep))
    fixture.collect(project, fixture.configurationName, fixture.variantName)

    val moduleIdentifier = DefaultModuleIdentifier.newId("io.sentry", "sentry-android-core")
    assertTrue {
      fixture.getSentryModules().size == 1 &&
        fixture.getSentryModules()[moduleIdentifier] == SemVer()
    }
    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] Detected Sentry modules {io.sentry:sentry-android-core=unspecified} " +
          "for variant: debug, config: debugRuntimeClasspath"
    }
  }

  @Test
  fun `sentry-android performance version - logs a info and persists module in build service`() {
    val sentryAndroidDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(
            DefaultModuleVersionIdentifier.newId("io.sentry", "sentry-android-core", "4.1.0")
          )
      }

    val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sentryAndroidDep))
    fixture.collect(project, fixture.configurationName, fixture.variantName)

    val moduleIdentifier = DefaultModuleIdentifier.newId("io.sentry", "sentry-android-core")
    assertTrue {
      fixture.getSentryModules().size == 1 &&
        fixture.getSentryModules()[moduleIdentifier] == SemVer(4, 1, 0)
    }
    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] Detected Sentry modules {io.sentry:sentry-android-core=4.1.0} " +
          "for variant: debug, config: debugRuntimeClasspath"
    }
  }

  @Test
  fun `non sentry dependencies are provided via the modules provider`() {
    val group = "androidx.sqlite"
    val name = "sqlite-framework"
    val moduleIdentifier = DefaultModuleIdentifier.newId(group, name)
    val version = "2.3.0"

    val sqliteDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(DefaultModuleVersionIdentifier.newId(group, name, version))
      }

    val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sqliteDep))
    fixture.collect(project, fixture.configurationName, fixture.variantName)

    assertTrue { fixture.getExternalModules()[moduleIdentifier]!! == SemVer.parse(version) }
  }

  @Test
  fun `sentry-android transitive - logs a info and persists both modules in build service`() {
    val firstLevelDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(DefaultModuleVersionIdentifier.newId("io.sentry", "sentry-android", "5.5.0"))
      }
    val transitiveSentryDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(
            DefaultModuleVersionIdentifier.newId("io.sentry", "sentry-android-core", "5.5.0")
          )
      }
    val okHttpDep =
      mock<ResolvedComponentResult> {
        whenever(mock.moduleVersion)
          .thenReturn(
            DefaultModuleVersionIdentifier.newId("io.sentry", "sentry-android-okhttp", "6.0.0")
          )
      }

    val project =
      fixture.getSut(
        testProjectDir.root,
        dependencies = setOf(firstLevelDep, transitiveSentryDep, okHttpDep),
      )
    fixture.collect(project, fixture.configurationName, fixture.variantName)

    val sentryAndroidModule = DefaultModuleIdentifier.newId("io.sentry", "sentry-android")
    val sentryAndroidCoreModule = DefaultModuleIdentifier.newId("io.sentry", "sentry-android-core")
    val sentryAndroidOkHttpModule =
      DefaultModuleIdentifier.newId("io.sentry", "sentry-android-okhttp")
    assertTrue {
      fixture.getSentryModules().size == 3 &&
        fixture.getSentryModules()[sentryAndroidModule] == SemVer(5, 5, 0) &&
        fixture.getSentryModules()[sentryAndroidCoreModule] == SemVer(5, 5, 0) &&
        fixture.getSentryModules()[sentryAndroidOkHttpModule] == SemVer(6, 0, 0)
    }
    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] Detected Sentry modules {io.sentry:sentry-android=5.5.0, " +
          "io.sentry:sentry-android-core=5.5.0, io.sentry:sentry-android-okhttp=6.0.0} " +
          "for variant: debug, config: debugRuntimeClasspath"
    }
  }
}
