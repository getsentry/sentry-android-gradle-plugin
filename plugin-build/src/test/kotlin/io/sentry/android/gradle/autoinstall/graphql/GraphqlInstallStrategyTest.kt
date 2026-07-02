package io.sentry.android.gradle.autoinstall.graphql

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
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.Logger

class GraphqlInstallStrategyTest {
  class Fixture {
    val logger = CapturingTestLogger()
    val dependencies = mock<DirectDependenciesMetadata>()
    val metadataDetails = mock<ComponentMetadataDetails>()
    val metadataContext =
      mock<ComponentMetadataContext> {
        whenever(it.details).thenReturn(metadataDetails)
        val metadata = mock<VariantMetadata>()
        doAnswer { (it.arguments[0] as Action<DirectDependenciesMetadata>).execute(dependencies) }
          .whenever(metadata)
          .withDependencies(any<Action<DirectDependenciesMetadata>>())

        doAnswer {
            // trigger the callback registered in tests
            (it.arguments[0] as Action<VariantMetadata>).execute(metadata)
          }
          .whenever(metadataDetails)
          .allVariants(any<Action<VariantMetadata>>())
      }

    fun getSut(graphqlVersion: String = "2.0.0"): GraphqlInstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(graphqlVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      return GraphqlInstallStrategyImpl(autoInstallEnabled = true, sentryVersion = "6.25.2", logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `installs sentry-graphql with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-graphql was successfully installed with version: 6.25.2"
    }
    verify(fixture.dependencies)
      .add(org.mockito.kotlin.check<String> { assertEquals("io.sentry:sentry-graphql:6.25.2", it) })
  }

  @Test
  fun `when graphql version is too high logs a message and does nothing`() {
    val sut = fixture.getSut(graphqlVersion = "22.1")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-graphql won't be installed because the current " +
          "version (22.1.0) is higher than the maximum supported version (21.9999.9999)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  private class GraphqlInstallStrategyImpl(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
    logger: Logger,
  ) : GraphqlInstallStrategy(autoInstallEnabled, sentryVersion, logger)
}
