package io.sentry.android.gradle.autoinstall.log4j2

import com.nhaarman.mockitokotlin2.any
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

class Log4j2InstallStrategyTest {
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

    fun getSut(log4j2Version: String = "2.0.0"): Log4j2InstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(log4j2Version) }
      whenever(metadataDetails.id).thenReturn(id)

      with(AutoInstallState.getInstance()) {
        this.enabled = true
        this.sentryVersion = "6.25.2"
      }
      return Log4j2InstallStrategyImpl(logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when log4j2 version is unsupported logs a message and does nothing`() {
    val sut = fixture.getSut(log4j2Version = "1.0.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-log4j2 won't be installed because the current " +
          "version is lower than the minimum supported version (2.0.0)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `installs sentry-log4j2 with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-log4j2 was successfully installed with version: 6.25.2"
    }
    verify(fixture.dependencies)
      .add(
        com.nhaarman.mockitokotlin2.check<String> {
          assertEquals("io.sentry:sentry-log4j2:6.25.2", it)
        }
      )
  }

  private class Log4j2InstallStrategyImpl(logger: Logger) : Log4j2InstallStrategy(logger)
}
