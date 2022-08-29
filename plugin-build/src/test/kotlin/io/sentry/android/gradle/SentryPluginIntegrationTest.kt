package io.sentry.android.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

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
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

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
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

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
