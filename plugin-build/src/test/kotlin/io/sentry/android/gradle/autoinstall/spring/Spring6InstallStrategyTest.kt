package io.sentry.android.gradle.autoinstall.spring

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

class Spring6InstallStrategyTest {
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

    fun getSut(springVersion: String = "6.0.0"): Spring6InstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(springVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      with(AutoInstallState.getInstance()) {
        this.enabled = true
        this.sentryVersion = "6.21.0"
      }
      return Spring6InstallStrategyImpl(logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when spring version is too low logs a message and does nothing`() {
    val sut = fixture.getSut(springVersion = "5.7.4")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-spring-jakarta won't be installed because the current " +
          "version is lower than the minimum supported version (6.0.0)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `when spring version is too high logs a message and does nothing`() {
    val sut = fixture.getSut(springVersion = "7.0.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-spring-jakarta won't be installed because the current " +
          "version is higher than the maximum supported version (6.9999.9999)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `installs sentry-spring-jakarta with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-spring-jakarta was successfully installed with version: 6.21.0"
    }
    verify(fixture.dependencies)
      .add(
        com.nhaarman.mockitokotlin2.check<String> {
          assertEquals("io.sentry:sentry-spring-jakarta:6.21.0", it)
        }
      )
  }

  private class Spring6InstallStrategyImpl(logger: Logger) : Spring6InstallStrategy(logger)
}
