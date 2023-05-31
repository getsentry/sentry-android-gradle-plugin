package io.sentry.android.gradle

import kotlin.test.assertEquals
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertTrue

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginIntegrationTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun uploadWithoutSentryCliProperties() {
        if (System.getenv("SENTRY_URL").isNullOrBlank()) {
            return // Don't run test if local test server endpoint is not set
        }
        sentryPropertiesFile.writeText("")
        applyAutoUploadProguardMappingWithCredentials()

        val build = runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertEquals(
            build.task(":app:uploadSentryProguardMappingsRelease")?.outcome,
            TaskOutcome.SUCCESS
        )
        assertTrue { "> Authorization: Bearer <token>" in build.output }
    }

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

    @Test
    fun uploadSourceContexts() {
        if (System.getenv("SENTRY_URL").isNullOrBlank()) {
            return // Don't run test if local test server endpoint is not set
        }
        applyUploadSourceContexts()

        testProjectDir.withDummyKtFile()
        /* ktlint-disable max-line-length */
        val uploadedIdRegex = """\w+":\{"state":"ok","missingChunks":\[],"uploaded_id":"(\w+-\w+-\w+-\w+-\w+)""".toRegex()
        /* ktlint-enable max-line-length */

        val build = runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertEquals(
            build.task(":app:sentryUploadSourceBundleRelease")?.outcome,
            TaskOutcome.SUCCESS
        )

        val uploadedId = uploadedIdRegex.find(build.output)?.groupValues?.get(1)
        val bundledId = verifySourceContextId(testProjectDir.root).toString()
        assertEquals(uploadedId, bundledId)
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

    private fun applyAutoUploadProguardMappingWithCredentials() {
        appBuildFile.appendText(
            // language=Groovy
            """
                sentry {
                  debug = true
                  includeProguardMapping = true
                  autoUploadProguardMapping = true
                  uploadNativeSymbols = false
                  org = 'sentry-sdks'
                  project = 'sentry-android'
                  authToken = '<token>'
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

    private fun applyUploadSourceContexts() {
        appBuildFile.appendText(
            // language=Groovy
            """
                sentry {
                  debug = true
                  includeSourceContext = true
                  includeProguardMapping = false
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """.trimIndent()
        )
    }
}
