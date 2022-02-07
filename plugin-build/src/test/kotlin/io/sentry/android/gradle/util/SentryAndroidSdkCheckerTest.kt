package io.sentry.android.gradle.util

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import java.io.File
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryAndroidSdkCheckerTest {

    class Fixture {

        private val configuration: Configuration = mock()
        private val resolvedConfiguration: ResolvedConfiguration = mock()

        val configurationName: String = "debugRuntimeClasspath"
        val variantName: String = "debug"

        val logger = CapturingTestLogger()

        fun getSut(
            tmpDir: File,
            resolveConfigurationError: Boolean = false,
            dependencies: Set<ResolvedDependency> = emptySet()
        ): Project {
            whenever(configuration.name).thenReturn(configurationName)
            whenever(configuration.resolvedConfiguration).thenReturn(resolvedConfiguration)
            whenever(resolvedConfiguration.firstLevelModuleDependencies).thenReturn(dependencies)
            whenever(resolvedConfiguration.hasError()).thenReturn(resolveConfigurationError)

            val fakeProject = ProjectBuilder
                .builder()
                .withProjectDir(tmpDir)
                .build()

            val project = spy(fakeProject)
            whenever(project.logger).thenReturn(logger)
            project.configurations.add(configuration)

            return project
        }
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val fixture = Fixture()

//    @Test
//    fun `configuration cannot be found - logs a warning and returns MISSING state`() {
//        val state = fixture.getSut(testProjectDir.root)
//            .detectSentryAndroidSdk("releaseRuntimeClasspath", "release")
//
//        assertTrue { state == SentryAndroidSdkState.MISSING }
//        assertTrue {
//            fixture.logger.capturedMessage ==
//                "[sentry] Unable to find configuration releaseRuntimeClasspath for variant release."
//        }
//    }
//
//    @Test
//    fun `resolvedConfiguration has error - logs a warning and returns MISSING state`() {
//        val state = fixture.getSut(testProjectDir.root, true)
//            .detectSentryAndroidSdk(fixture.configurationName, fixture.variantName)
//
//        assertTrue { state == SentryAndroidSdkState.MISSING }
//        assertTrue {
//            fixture.logger.capturedMessage ==
//                "[sentry] Unable to resolve configuration debugRuntimeClasspath."
//        }
//    }
//
//    @Test
//    fun `sentry-android is not in the dependencies - logs a warning and returns MISSING state`() {
//        val sqliteDep = mock<ResolvedDependency>()
//        whenever(sqliteDep.moduleGroup).doReturn("androidx.sqlite")
//        whenever(sqliteDep.moduleName).doReturn("sqlite")
//
//        val state = fixture.getSut(testProjectDir.root, dependencies = setOf(sqliteDep))
//            .detectSentryAndroidSdk(fixture.configurationName, fixture.variantName)
//
//        assertTrue { state == SentryAndroidSdkState.MISSING }
//        assertTrue {
//            fixture.logger.capturedMessage ==
//                "[sentry] sentry-android dependency was not found."
//        }
//    }
//
//    @Test
//    fun `sentry-android as a local dependency - logs a info and returns MISSING state`() {
//        val sentryAndroidDep = mock<ResolvedDependency>()
//        whenever(sentryAndroidDep.moduleGroup).doReturn("io.sentry")
//        whenever(sentryAndroidDep.moduleName).doReturn("sentry-android-core")
//        // this is the case when sentry-android is a local dependency
//        whenever(sentryAndroidDep.moduleVersion).doReturn("unspecified")
//
//        val state = fixture.getSut(testProjectDir.root, dependencies = setOf(sentryAndroidDep))
//            .detectSentryAndroidSdk(fixture.configurationName, fixture.variantName)
//
//        assertTrue { state == SentryAndroidSdkState.MISSING }
//        assertTrue {
//            fixture.logger.capturedMessage ==
//                "[sentry] Detected sentry-android MISSING for version: unspecified, " +
//                "variant: debug, config: debugRuntimeClasspath"
//        }
//    }
//
//    @Test
//    fun `sentry-android performance version - logs a info and returns PERFORMANCE state`() {
//        val sentryAndroidDep = mock<ResolvedDependency>()
//        whenever(sentryAndroidDep.moduleGroup).doReturn("io.sentry")
//        whenever(sentryAndroidDep.moduleName).doReturn("sentry-android-core")
//        whenever(sentryAndroidDep.moduleVersion).doReturn("4.1.0")
//
//        val state = fixture.getSut(testProjectDir.root, dependencies = setOf(sentryAndroidDep))
//            .detectSentryAndroidSdk(fixture.configurationName, fixture.variantName)
//
//        assertTrue { state == SentryAndroidSdkState.PERFORMANCE }
//        assertTrue {
//            fixture.logger.capturedMessage ==
//                "[sentry] Detected sentry-android PERFORMANCE for version: 4.1.0, " +
//                "variant: debug, config: debugRuntimeClasspath"
//        }
//    }
//
//    @Test
//    fun `sentry-android transitive - logs a info and returns FILE_IO state`() {
//        val firstLevelDep = mock<ResolvedDependency>()
//        whenever(firstLevelDep.moduleGroup).doReturn("io.sentry")
//        whenever(firstLevelDep.moduleName).doReturn("sentry-android")
//
//        val transitiveSentryDep = mock<ResolvedDependency>()
//        whenever(transitiveSentryDep.moduleGroup).doReturn("io.sentry")
//        whenever(transitiveSentryDep.moduleName).doReturn("sentry-android-core")
//        whenever(transitiveSentryDep.moduleVersion).doReturn("5.5.0")
//        whenever(firstLevelDep.children).thenReturn(setOf(transitiveSentryDep))
//
//        val state = fixture.getSut(testProjectDir.root, dependencies = setOf(firstLevelDep))
//            .detectSentryAndroidSdk(fixture.configurationName, fixture.variantName)
//
//        assertTrue { state == SentryAndroidSdkState.FILE_IO }
//        assertTrue {
//            fixture.logger.capturedMessage ==
//                "[sentry] Detected sentry-android FILE_IO for version: 5.5.0, " +
//                "variant: debug, config: debugRuntimeClasspath"
//        }
//    }
}
