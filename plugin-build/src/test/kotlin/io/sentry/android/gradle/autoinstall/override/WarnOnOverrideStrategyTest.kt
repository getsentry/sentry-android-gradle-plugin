// ktlint-disable max-line-length
package io.sentry.android.gradle.autoinstall.override

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.util.SentryModules
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger

class WarnOnOverrideStrategyTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val metadataDetails = mock<ComponentMetadataDetails>()
        val metadataContext = mock<ComponentMetadataContext> {
            whenever(it.details).thenReturn(metadataDetails)
        }

        fun getSut(
            enabled: Boolean = true,
            userDefinedVersion: String = "6.0.0"
        ): WarnOnOverrideStrategy {
            val id = mock<ModuleVersionIdentifier> {
                whenever(it.module).doReturn(SentryModules.SENTRY_ANDROID)
                whenever(it.version).doReturn(userDefinedVersion)
            }
            whenever(metadataDetails.id).thenReturn(id)

            with(AutoInstallState.getInstance()) {
                this.enabled = enabled
                this.sentryVersion = "6.34.0"
            }
            return WarnOnOverrideStrategyImpl(logger)
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
            fixture.logger.capturedMessage
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

    private class WarnOnOverrideStrategyImpl(logger: Logger) : WarnOnOverrideStrategy(logger)
}
