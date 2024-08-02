package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion

class SentryPluginTelemetryTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  @Test
  fun `telemetry can be disabled`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            sentry {
              telemetry = false
            }
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug", "--info").build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
    assertTrue(result.output) { "Sentry telemetry has been disabled." in result.output }
    assertFalse(result.output) { "sentry-cli" in result.output }
  }

  @Test
  fun `telemetry is enabled by default`() {
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
            }
            """
        .trimIndent()
    )
    val result = runner.appendArguments("app:assembleDebug", "--info").build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
    assertTrue(result.output) { "Sentry telemetry is enabled." in result.output }
  }
}
