package io.sentry.android.gradle.autoinstall.timber

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
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

class TimberInstallStrategyTest {
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

    fun getSut(timberVersion: String = "4.7.1"): TimberInstallStrategy {
      val id = mock<ModuleVersionIdentifier> { whenever(it.version).doReturn(timberVersion) }
      whenever(metadataDetails.id).thenReturn(id)

      with(AutoInstallState.getInstance()) {
        this.enabled = true
        this.sentryVersion = "5.6.1"
      }
      return TimberInstallStrategyImpl(logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when timber version is unsupported logs a message and does nothing`() {
    val sut = fixture.getSut(timberVersion = "4.5.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-android-timber won't be installed because the current " +
          "version (4.5.0) is lower than the minimum supported version (4.6.0)"
    }
    verify(fixture.metadataDetails, never()).allVariants(any())
  }

  @Test
  fun `installs sentry-android-timber with info message`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] sentry-android-timber was successfully installed with version: 5.6.1"
    }
    verify(fixture.dependencies)
      .add(check<String> { assertEquals("io.sentry:sentry-android-timber:5.6.1", it) })
  }

  private class TimberInstallStrategyImpl(logger: Logger) : TimberInstallStrategy(logger)
}
