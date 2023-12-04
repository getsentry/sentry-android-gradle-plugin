package io.sentry.android.gradle.autoinstall.sqlite

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

class SqliteInstallStrategyTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = mock<DirectDependenciesMetadata>()
        val metadataDetails = mock<ComponentMetadataDetails>()
        val metadataContext = mock<ComponentMetadataContext> {
            whenever(it.details).thenReturn(metadataDetails)
            val metadata = mock<VariantMetadata>()
            doAnswer {
                (it.arguments[0] as Action<DirectDependenciesMetadata>).execute(dependencies)
            }.whenever(metadata).withDependencies(any<Action<DirectDependenciesMetadata>>())

            doAnswer {
                // trigger the callback registered in tests
                (it.arguments[0] as Action<VariantMetadata>).execute(metadata)
            }.whenever(metadataDetails).allVariants(any<Action<VariantMetadata>>())
        }

        fun getSut(
            sqliteVersion: String = "2.0.0"
        ): SQLiteInstallStrategy {
            val id = mock<ModuleVersionIdentifier> {
                whenever(it.version).doReturn(sqliteVersion)
            }
            whenever(metadataDetails.id).thenReturn(id)

            with(AutoInstallState.getInstance()) {
                this.enabled = true
                this.sentryVersion = "6.21.0"
            }
            return SQLiteInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sqlite version is unsupported logs a message and does nothing`() {
        val sut = fixture.getSut(sqliteVersion = "1.0.0")
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-sqlite won't be installed because the current " +
                "version is lower than the minimum supported version (2.0.0)"
        }
        verify(fixture.metadataDetails, never()).allVariants(any())
    }

    @Test
    fun `installs sentry-android-sqlite with info message`() {
        val sut = fixture.getSut()
        sut.execute(fixture.metadataContext)

        assertTrue {
            fixture.logger.capturedMessage ==
                "[sentry] sentry-android-sqlite was successfully installed with version: 6.21.0"
        }
        verify(fixture.dependencies).add(
            com.nhaarman.mockitokotlin2.check<String> {
                assertEquals("io.sentry:sentry-android-sqlite:6.21.0", it)
            }
        )
    }

    private class SQLiteInstallStrategyImpl(logger: Logger) : SQLiteInstallStrategy(logger)
}
