package io.sentry.android.gradle.autoinstall.okhttp

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.api.Action
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.DirectDependenciesMetadata
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.VariantMetadata
import org.junit.Test
import org.slf4j.Logger

class OkHttpInstallStrategyTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = mock<DirectDependenciesMetadata>()
        val metadataDetails = mock<ComponentMetadataDetails>()
        val metadataContext = mock<ComponentMetadataContext> {
            whenever(it.details).thenReturn(metadataDetails)
            val metadata = mock<VariantMetadata>()
            doAnswer {
                (it.arguments[0] as Action<DirectDependenciesMetadata>).execute(dependencies)
            }.whenever(metadata).withDependencies(any<Action<DirectDependenciesMetadata>>())

            doAnswer {
                // trigger the callback registered in tests
                (it.arguments[0] as Action<VariantMetadata>).execute(metadata)
            }.whenever(metadataDetails).allVariants(any<Action<VariantMetadata>>())
        }

        fun getSut(
            installOkHttp: Boolean = true,
            okHttpVersion: String = "4.9.3"
        ): OkHttpInstallStrategy {
            val id = mock<ModuleVersionIdentifier> {
                whenever(it.version).doReturn(okHttpVersion)
            }
            whenever(metadataDetails.id).thenReturn(id)

            with(AutoInstallState.getInstance()) {
                this.installOkHttp = installOkHttp
                this.sentryVersion = "5.6.1"
            }
            return OkHttpInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-android-okhttp is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installOkHttp = false)
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-okhttp won't be installed because it was already " +
                "installed directly"
        }
        verify(fixture.metadataContext, never()).details
    }

    @Test
    fun `when okhttp version is unsupported logs a message and does nothing`() {
        val sut = fixture.getSut(okHttpVersion = "3.11.0")
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-okhttp won't be installed because the current " +
                "version is lower than the minimum supported version (3.13.0)"
        }
        verify(fixture.metadataDetails, never()).allVariants(any())
    }

    @Test
    fun `installs sentry-android-okhttp with info message`() {
        val sut = fixture.getSut()
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-okhttp was successfully installed with version: 5.6.1"
        }
        verify(fixture.dependencies).add(
            check<String> {
                assertEquals("io.sentry:sentry-android-okhttp:5.6.1", it)
            }
        )
    }

    private class OkHttpInstallStrategyImpl(logger: Logger) : OkHttpInstallStrategy(logger)
}
