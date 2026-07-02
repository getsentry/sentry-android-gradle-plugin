package io.sentry.android.gradle.autoinstall.spring

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

class SpringBoot2InstallStrategyTest {
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

    fun getSut(springVersion: String = "2.1.0"): SpringBoot2InstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(springVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      return SpringBoot2InstallStrategyImpl(
        autoInstallEnabled = true,
        sentryVersion = "6.28.0",
        logger,
      )
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when spring version is too low logs a message and does nothing`() {
    val sut = fixture.getSut(springVersion = "2.0.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-spring-boot won't be installed because the current " +
          "version (2.0.0) is lower than the minimum supported version (2.1.0)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `when spring version is too high logs a message and does nothing`() {
    val sut = fixture.getSut(springVersion = "3.0.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-spring-boot won't be installed because the current " +
          "version (3.0.0) is higher than the maximum supported version (2.9999.9999)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `installs sentry-spring-boot with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-spring-boot was successfully installed with version: " + "6.28.0"
    }
    verify(fixture.dependencies)
      .add(
        org.mockito.kotlin.check<String> { assertEquals("io.sentry:sentry-spring-boot:6.28.0", it) }
      )
  }

  private class SpringBoot2InstallStrategyImpl(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
    logger: Logger,
  ) : SpringBoot2InstallStrategy(autoInstallEnabled, sentryVersion, logger)
}
