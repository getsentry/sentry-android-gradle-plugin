package io.sentry.android.gradle.autoinstall.fragment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doAnswer
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
import org.gradle.api.artifacts.VariantMetadata
import org.gradle.api.internal.provider.DefaultProvider
import org.junit.Test
import org.slf4j.Logger

class FragmentInstallStrategyTest {
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

        fun getSut(installFragment: Boolean = true): FragmentInstallStrategy {
            with(AutoInstallState.getInstance()) {
                this.installFragment = installFragment
                this.sentryVersion = "5.6.1"
            }
            return FragmentInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-android-fragment is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installFragment = false)
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-fragment won't be installed because it was already " +
                "installed directly"
        }
        verify(fixture.metadataContext, never()).details
    }

    @Test
    fun `installs sentry-android-fragment with info message`() {
        val sut = fixture.getSut()
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-fragment was successfully installed with version: 5.6.1"
        }
        verify(fixture.dependencies).add(
            check<String> {
                assertEquals("io.sentry:sentry-android-fragment:5.6.1", it)
            }
        )
    }

    private class FragmentInstallStrategyImpl(logger: Logger) : FragmentInstallStrategy(logger)
}
