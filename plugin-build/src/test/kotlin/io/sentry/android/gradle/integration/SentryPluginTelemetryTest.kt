package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion

class SentryPluginTelemetryTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

    @Ignore
    @Test
    fun `telemetry can be disabled`() {
        appBuildFile.appendText(
            // language=Groovy
            """
                sentry {
                  telemetry = false
                }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleDebug", "--debug")
            .build()

        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
        assertTrue(result.output) { "Sentry telemetry has been disabled." in result.output }
    }

    @Ignore
    @Test
    fun `telemetry is enabled by default`() {
        val result = runner
            .appendArguments("app:assembleDebug", "--info")
            .build()

        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
        assertTrue(result.output) { "Sentry telemetry is enabled." in result.output }
    }
}
