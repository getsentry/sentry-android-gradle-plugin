package io.sentry.android.gradle.autoinstall.override

import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.util.SentryModules
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.Logger

class WarnOnOverrideStrategyTest {
  class Fixture {
    val logger = CapturingTestLogger()
    val metadataDetails = mock<ComponentMetadataDetails>()
    val metadataContext =
      mock<ComponentMetadataContext> { whenever(it.details).thenReturn(metadataDetails) }

    fun getSut(
      enabled: Boolean = true,
      userDefinedVersion: String = "6.0.0",
    ): WarnOnOverrideStrategy {
      val id =
        mock<ModuleVersionIdentifier> {
          whenever(it.module).doReturn(SentryModules.SENTRY_ANDROID)
          whenever(it.version).doReturn(userDefinedVersion)
        }
      whenever(metadataDetails.id).thenReturn(id)

      return WarnOnOverrideStrategyImpl(
        autoInstallEnabled = enabled,
        sentryVersion = "6.34.0",
        logger,
      )
    }
  }

  private val fixture = Fixture()

  @Test
  fun `when autoInstall is disabled does nothing`() {
    val sut = fixture.getSut(enabled = false)
    sut.execute(fixture.metadataContext)

    verify(fixture.metadataContext, never()).details
  }

  @Test
  fun `when unknown version does nothing`() {
    val sut = fixture.getSut(userDefinedVersion = "whatever")
    sut.execute(fixture.metadataContext)

    assertEquals(
      "[sentry] Unable to parse version whatever as a semantic version.",
      fixture.logger.capturedMessage,
    )
  }

  @Test
  fun `when user defined version is higher than the plugin version does nothing`() {
    val sut = fixture.getSut(userDefinedVersion = "7.0.0")
    sut.execute(fixture.metadataContext)

    assertNull(fixture.logger.capturedMessage)
  }

  @Test
  fun `when user defined version is lower than the plugin version prints a warning`() {
    val sut = fixture.getSut(userDefinedVersion = "6.0.0")
    sut.execute(fixture.metadataContext)

    assertTrue {
      fixture.logger.capturedMessage ==
        "WARNING: Version of 'io.sentry:sentry-android' was overridden from '6.0.0' to '6.34.0' by the Sentry Gradle plugin. If you want to use the older version, you can add `autoInstallation.sentryVersion.set(\"6.0.0\")` in the `sentry {}` plugin configuration block"
    }
  }

  private class WarnOnOverrideStrategyImpl(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
    logger: Logger,
  ) : WarnOnOverrideStrategy(autoInstallEnabled, sentryVersion, logger)
}
