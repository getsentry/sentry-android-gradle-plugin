package io.sentry.android.gradle.autoinstall.graphql

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.util.SemVer
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

class Graphql22InstallStrategyTest {
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
            graphqlVersion: String = "22.0"
        ): Graphql22InstallStrategy {
            val id = mock<ModuleVersionIdentifier> {
                whenever(it.version).doReturn(graphqlVersion)
            }
            whenever(metadataDetails.id).thenReturn(id)

            with(AutoInstallState.getInstance()) {
                this.enabled = true
                this.sentryVersion = "8.0.0"
            }
            return Graphql22InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `installs sentry-graphql with info message`() {
        val sut = fixture.getSut()
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-graphql-22 was successfully installed with version: 8.0.0"
        }
        verify(fixture.dependencies).add(
            com.nhaarman.mockitokotlin2.check<String> {
                assertEquals("io.sentry:sentry-graphql-22:8.0.0", it)
            }
        )
    }

    @Test
    fun `when graphql version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(graphqlVersion = "21.9")
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-graphql-22 won't be installed because the current " +
                "version is lower than the minimum supported version (22.0.0)"
        }
        verify(fixture.metadataDetails, never()).allVariants(any())
    }

    private class Graphql22InstallStrategyImpl(logger: Logger) : Graphql22InstallStrategy(logger)
}
