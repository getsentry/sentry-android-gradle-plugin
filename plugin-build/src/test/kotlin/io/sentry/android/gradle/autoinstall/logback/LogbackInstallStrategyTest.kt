package io.sentry.android.gradle.autoinstall.logback

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

class LogbackInstallStrategyTest {
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

    fun getSut(logbackVersion: String = "2.0.0"): LogbackInstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(logbackVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      return LogbackInstallStrategyImpl(autoInstallEnabled = true, sentryVersion = "6.25.2", logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when logback version is unsupported logs a message and does nothing`() {
    val sut = fixture.getSut(logbackVersion = "0.0.1")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-logback won't be installed because the current " +
          "version (0.0.1) is lower than the minimum supported version (1.0.0)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `installs sentry-logback with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-logback was successfully installed with version: 6.25.2"
    }
    verify(fixture.dependencies)
      .add(org.mockito.kotlin.check<String> { assertEquals("io.sentry:sentry-logback:6.25.2", it) })
  }

  private class LogbackInstallStrategyImpl(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
    logger: Logger,
  ) : LogbackInstallStrategy(autoInstallEnabled, sentryVersion, logger)
}
