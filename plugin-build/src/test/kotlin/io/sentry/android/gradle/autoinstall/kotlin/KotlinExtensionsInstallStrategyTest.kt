package io.sentry.android.gradle.autoinstall.kotlin

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
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.Logger

class KotlinExtensionsInstallStrategyTest {
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

    fun getSut(kotlinExtensionsVersion: String = "1.6.1"): KotlinExtensionsInstallStrategy {
      val id =
        mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(kotlinExtensionsVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      with(AutoInstallState.getInstance()) {
        this.enabled = true
        this.sentryVersion = "6.25.2"
      }
      return KotlinExtensionsInstallStrategyImpl(logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when kotlinExtensions version is unsupported logs a message and does nothing`() {
    val sut = fixture.getSut(kotlinExtensionsVersion = "1.6.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-kotlin-extensions won't be installed because the current " +
          "version (1.6.0) is lower than the minimum supported version (1.6.1)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `installs sentry-kotlin-extensions with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-kotlin-extensions was successfully installed with version: 6.25.2"
    }
    verify(fixture.dependencies)
      .add(
        org.mockito.kotlin.check<String> {
          assertEquals("io.sentry:sentry-kotlin-extensions:6.25.2", it)
        }
      )
  }

  private class KotlinExtensionsInstallStrategyImpl(logger: Logger) :
    KotlinExtensionsInstallStrategy(logger)
}
