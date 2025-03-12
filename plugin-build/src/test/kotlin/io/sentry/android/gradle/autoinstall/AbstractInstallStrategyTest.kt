package io.sentry.android.gradle.autoinstall

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import kotlin.test.assertTrue
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.junit.Test
import org.slf4j.Logger

class AbstractInstallStrategyTest {
  class Fixture {
    val logger = CapturingTestLogger()
    val metadataDetails = mock<ComponentMetadataDetails>()
    val metadataContext =
      mock<ComponentMetadataContext> { whenever(it.details).thenReturn(metadataDetails) }

    fun getSut(): AbstractInstallStrategy {
      with(AutoInstallState.getInstance()) { this.enabled = false }
      return RandomInstallStrategy(logger)
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when autoInstallation is disabled does nothing`() {
    val sut = fixture.getSut()
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "[sentry] random-module won't be installed because autoInstallation is disabled"
    }
    verify(fixture.metadataContext, never()).details
  }

  private class RandomInstallStrategy(
    logger: Logger,
    override val sentryModuleId: String = "random-module",
  ) : AbstractInstallStrategy() {
    init {
      this.logger = logger
    }
  }
}
