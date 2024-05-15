package io.sentry.android.gradle.telemetry

import io.sentry.android.gradle.SentryCliProvider
import kotlin.test.assertEquals
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryTelemetryServiceTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    @Suppress("UnstableApiUsage")
    @Test
    fun `when no auth token is present, SentryCliInfoValueSource sentry-cli info returns an empty string`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        val cliPath = SentryCliProvider
            .getCliResourcesExtractionPath(project.layout.buildDirectory.asFile.get())

        val infoOutput = project.providers.of(SentryCliInfoValueSource::class.java) { cliVS ->
            cliVS.parameters.buildDirectory.set(project.buildDir)
            cliVS.parameters.cliExecutable.set(cliPath.absolutePath)
            // notice the absence of the auth token
            // cliVS.parameters.authToken.set("")
        }.get()

        assertEquals("", infoOutput)
    }
}
