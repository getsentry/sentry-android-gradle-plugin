package io.sentry.android.gradle.util

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.services.SentrySdkStateHolder
import java.io.File
import kotlin.test.assertTrue
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryAndroidSdkCheckerTest {

    class Fixture {

        private val configuration = mock<Configuration>()
        val resolvableDependencies = mock<ResolvableDependencies>()
        private val resolutionResult = mock<ResolutionResult>()

        val configurationName: String = "debugRuntimeClasspath"
        val variantName: String = "debug"

        lateinit var sdkStateHolderProvider: Provider<SentrySdkStateHolder>
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

            sdkStateHolderProvider = SentrySdkStateHolder.register(project)

            return project
        }

        fun getSdkState() = sdkStateHolderProvider.get().sdkState
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val fixture = Fixture()

    @Test
    fun `configuration cannot be found - logs a warning and returns MISSING state`() {
        val project = fixture.getSut(testProjectDir.root)
        project.detectSentryAndroidSdk(
            "releaseRuntimeClasspath",
            "release",
            fixture.sdkStateHolderProvider
        )

        assertTrue { fixture.getSdkState() == SentryAndroidSdkState.MISSING }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] Unable to find configuration releaseRuntimeClasspath for variant release."
        }
    }

    @Test
    fun `sentry-android is not in the dependencies - logs a warning and returns MISSING state`() {
        val sqliteDep = mock<ResolvedComponentResult> {
            whenever(mock.moduleVersion).thenReturn(
                DefaultModuleVersionIdentifier.newId("androidx.sqlite", "sqlite", "2.1.0")
            )
        }

        val project = fixture.getSut(testProjectDir.root, dependencies = setOf(sqliteDep))
        project.detectSentryAndroidSdk(
            fixture.configurationName,
            fixture.variantName,
            fixture.sdkStateHolderProvider
        )

        assertTrue { fixture.getSdkState() == SentryAndroidSdkState.MISSING }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android dependency was not found."
        }
    }

    @Test
    fun `sentry-android as a local dependency - logs a info and returns MISSING state`() {
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
        project.detectSentryAndroidSdk(
            fixture.configurationName,
            fixture.variantName,
            fixture.sdkStateHolderProvider
        )

        assertTrue { fixture.getSdkState() == SentryAndroidSdkState.MISSING }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] Detected sentry-android MISSING for version: unspecified, " +
                "variant: debug, config: debugRuntimeClasspath"
        }
    }

    @Test
    fun `sentry-android performance version - logs a info and returns PERFORMANCE state`() {
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
        project.detectSentryAndroidSdk(
            fixture.configurationName,
            fixture.variantName,
            fixture.sdkStateHolderProvider
        )

        assertTrue { fixture.getSdkState() == SentryAndroidSdkState.PERFORMANCE }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] Detected sentry-android PERFORMANCE for version: 4.1.0, " +
                "variant: debug, config: debugRuntimeClasspath"
        }
    }

    @Test
    fun `sentry-android transitive - logs a info and returns FILE_IO state`() {
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

        val project = fixture.getSut(
            testProjectDir.root,
            dependencies = setOf(firstLevelDep, transitiveSentryDep)
        )
        project.detectSentryAndroidSdk(
            fixture.configurationName,
            fixture.variantName,
            fixture.sdkStateHolderProvider
        )

        assertTrue { fixture.getSdkState() == SentryAndroidSdkState.FILE_IO }
        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] Detected sentry-android FILE_IO for version: 5.5.0, " +
                "variant: debug, config: debugRuntimeClasspath"
        }
    }
}
