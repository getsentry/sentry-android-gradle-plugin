package io.sentry.android.gradle.util

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.services.SentryModulesService
import java.io.File
import kotlin.test.assertTrue
import org.gradle.api.Action
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

class SentryModulesCollectorTest {

    class Fixture {

        private val configuration = mock<Configuration>()
        val resolvableDependencies = mock<ResolvableDependencies>()
        private val resolutionResult = mock<ResolutionResult>()

        val configurationName: String = "debugRuntimeClasspath"
        val variantName: String = "debug"

        lateinit var sentryModulesServiceProvider: Provider<SentryModulesService>
        val logger = CapturingTestLogger()

        fun getSut(
            tmpDir: File,
            dependencies: Set<ResolvedComponentResult> = emptySet()
        ): Project {
            whenever(configuration.name).thenReturn(configurationName)
            whenever(configuration.incoming).thenReturn(resolvableDependencies)
            whenever(resolvableDependencies.resolutionResult).thenReturn(resolutionResult)
            whenever(resolutionResult.allComponents).thenReturn(dependencies)
            doAnswer {
                // trigger the callback registered in tests
                (it.arguments[0] as Action<ResolvableDependencies>).execute(resolvableDependencies)
            }
                .whenever(resolvableDependencies)
                .afterResolve(any<Action<ResolvableDependencies>>())

            val fakeProject = ProjectBuilder
                .builder()
                .withProjectDir(tmpDir)
                .build()

            val project = spy(fakeProject)
            whenever(project.logger).thenReturn(logger)
            project.configurations.add(configuration)

            sentryModulesServiceProvider = SentryModulesService.register(project)

            return project
        }

        fun getSentryModules() = sentryModulesServiceProvider.get().sentryModules

        fun getExternalModules() = sentryModulesServiceProvider.get().externalModules
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val fixture = Fixture()

    @Test
    fun `configuration cannot be found - logs a warning and the modules list is empty`() {
        val project = fixture.getSut(testProjectDir.root)
        project.collectModules(
            "releaseRuntimeClasspath",
            "release",
            fixture.sentryModulesServiceProvider
        )
        assertTrue { fixture.getSentryModules().isEmpty() }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] Unable to find configuration releaseRuntimeClasspath for variant release."
        }
    }

    @Test
    fun `sentry-android is not in the dependencies - modules list is empty`() {
        val sqliteDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId("androidx.sqlite", "sqlite", "2.1.0")
            )
        }

        val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sqliteDep))
        project.collectModules(
            fixture.configurationName,
            fixture.variantName,
            fixture.sentryModulesServiceProvider
        )

        assertTrue { fixture.getSentryModules().isEmpty() }
    }

    @Test
    fun `sentry-android as a local dependency - module's version is omitted`() {
        val sentryAndroidDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                // this is the case when sentry-android is a local dependency
                DefaultModuleVersionIdentifier.newId(
                    "io.sentry",
                    "sentry-android-core",
                    "unspecified"
                )
            )
        }

        val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sentryAndroidDep))
        project.collectModules(
            fixture.configurationName,
            fixture.variantName,
            fixture.sentryModulesServiceProvider
        )

        val moduleIdentifier = DefaultModuleIdentifier
            .newId("io.sentry", "sentry-android-core")
        assertTrue {
            fixture.getSentryModules().size == 1 &&
                fixture.getSentryModules()[moduleIdentifier] == SemVer()
        }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] Detected Sentry modules {io.sentry:sentry-android-core=0.0.0} " +
                "for variant: debug, config: debugRuntimeClasspath"
        }
    }

    @Test
    fun `sentry-android performance version - logs a info and persists module in build service`() {
        val sentryAndroidDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId(
                    "io.sentry",
                    "sentry-android-core",
                    "4.1.0"
                )
            )
        }

        val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sentryAndroidDep))
        project.collectModules(
            fixture.configurationName,
            fixture.variantName,
            fixture.sentryModulesServiceProvider
        )

        val moduleIdentifier = DefaultModuleIdentifier
            .newId("io.sentry", "sentry-android-core")
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

        val sqliteDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId(group, name, version)
            )
        }

        val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sqliteDep))
        project.collectModules(
            fixture.configurationName,
            fixture.variantName,
            fixture.sentryModulesServiceProvider
        )

        assertTrue {
            fixture.getExternalModules()[moduleIdentifier]!! == SemVer.parse(version)
        }
    }

    @Test
    fun `sentry-android transitive - logs a info and persists both modules in build service`() {
        val firstLevelDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId(
                    "io.sentry",
                    "sentry-android",
                    "5.5.0"
                )
            )
        }
        val transitiveSentryDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId(
                    "io.sentry",
                    "sentry-android-core",
                    "5.5.0"
                )
            )
        }
        val okHttpDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId(
                    "io.sentry",
                    "sentry-android-okhttp",
                    "6.0.0"
                )
            )
        }

        val project = fixture.getSut(
            testProjectDir.root,
            dependencies = setOf(firstLevelDep, transitiveSentryDep, okHttpDep)
        )
        project.collectModules(
            fixture.configurationName,
            fixture.variantName,
            fixture.sentryModulesServiceProvider
        )

        val sentryAndroidModule = DefaultModuleIdentifier
            .newId("io.sentry", "sentry-android")
        val sentryAndroidCoreModule = DefaultModuleIdentifier
            .newId("io.sentry", "sentry-android-core")
        val sentryAndroidOkHttpModule = DefaultModuleIdentifier
            .newId("io.sentry", "sentry-android-okhttp")
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
