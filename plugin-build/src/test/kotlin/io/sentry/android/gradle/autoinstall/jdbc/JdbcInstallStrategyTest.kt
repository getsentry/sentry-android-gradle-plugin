package io.sentry.android.gradle.autoinstall.jdbc

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.Logger

class JdbcInstallStrategyTest {
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

    fun getSut(jdbcVersion: String = "2.0.0"): JdbcInstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(jdbcVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      with(AutoInstallState.getInstance()) {
        this.enabled = true
        this.sentryVersion = "6.21.0"
      }
      return JdbcInstallStrategyImpl(logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `installs sentry-jdbc with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-jdbc was successfully installed with version: 6.21.0"
    }
    verify(fixture.dependencies)
      .add(org.mockito.kotlin.check<String> { assertEquals("io.sentry:sentry-jdbc:6.21.0", it) })
  }

  private class JdbcInstallStrategyImpl(logger: Logger) : JdbcInstallStrategy(logger)
}
