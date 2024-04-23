package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.verifySourceContextId
import io.sentry.android.gradle.withDummyComposeFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginIntegrationTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

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
        sentryPropertiesFile.appendText("auth.token=<token>")
        applyAutoUploadProguardMapping()

        val build = runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertEquals(
            build.task(":app:uploadSentryProguardMappingsRelease")?.outcome,
            TaskOutcome.SUCCESS
        )
        assertTrue(build.output) {
            "Most likely you have to update your self-hosted Sentry version " +
                "to get all of the latest features." in build.output
        }
    }

    @Test
    fun uploadNativeSymbols() {
        if (System.getenv("SENTRY_URL").isNullOrBlank()) {
            return // Don't run test if local test server endpoint is not set
        }
        sentryPropertiesFile.appendText("auth.token=<token>")
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
        sentryPropertiesFile.appendText("auth.token=<token>")
        applyUploadSourceContexts()

        testProjectDir.withDummyComposeFile()
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
                dependencies {
                    implementation 'androidx.fragment:fragment:1.3.5'
                }

                sentry {
                  debug = true
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
                  projectName = 'sentry-android'
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
