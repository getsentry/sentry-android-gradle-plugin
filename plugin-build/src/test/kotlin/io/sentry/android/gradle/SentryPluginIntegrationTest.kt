package io.sentry.android.gradle

import kotlin.test.assertEquals
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginIntegrationTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun uploadSentryProguardMappingsIntegration() {
        if (System.getenv("SENTRY_URL").isNullOrBlank()) {
            return // Don't run test if local test server endpoint is not set
        }
        applyAutoUploadProguardMapping()

        val build = runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertEquals(
            build.task(":app:uploadSentryProguardMappingsRelease")?.outcome,
            TaskOutcome.SUCCESS
        )
    }

    @Test
    fun uploadNativeSymbols() {
        if (System.getenv("SENTRY_URL").isNullOrBlank()) {
            return // Don't run test if local test server endpoint is not set
        }
        applyUploadNativeSymbols()

        val build = runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertEquals(
            build.task(":app:uploadSentryNativeSymbolsForRelease")?.outcome,
            TaskOutcome.SUCCESS
        )
    }

    private fun applyAutoUploadProguardMapping() {
        appBuildFile.appendText(
            // language=Groovy
            """
                sentry {
                  includeProguardMapping = true
                  autoUploadProguardMapping = true
                  uploadNativeSymbols = false
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """.trimIndent()
        )
    }

    private fun applyUploadNativeSymbols() {
        appBuildFile.appendText(
            // language=Groovy
            """
                sentry {
                  autoUploadProguardMapping = false
                  uploadNativeSymbols = true
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """.trimIndent()
        )
    }
}
