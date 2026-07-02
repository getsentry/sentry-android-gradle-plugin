package io.sentry.android.gradle.autoinstall.quartz

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.Logger

class QuartzInstallStrategyTest {
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

    fun getSut(quartzVersion: String = "2.0.0"): QuartzInstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(quartzVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      return QuartzInstallStrategyImpl(autoInstallEnabled = true, sentryVersion = "6.30.0", logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `installs sentry-quartz with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-quartz was successfully installed with version: 6.30.0"
    }
    verify(fixture.dependencies)
      .add(org.mockito.kotlin.check<String> { assertEquals("io.sentry:sentry-quartz:6.30.0", it) })
  }

  private class QuartzInstallStrategyImpl(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
    logger: Logger,
  ) : QuartzInstallStrategy(autoInstallEnabled, sentryVersion, logger)
}
