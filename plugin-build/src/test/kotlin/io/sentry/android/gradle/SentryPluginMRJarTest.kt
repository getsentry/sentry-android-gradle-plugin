package io.sentry.android.gradle

import io.sentry.android.gradle.integration.BaseSentryPluginTest
import kotlin.test.assertTrue
import org.junit.Test

class SentryPluginMRJarTest :
    BaseSentryPluginTest(androidGradlePluginVersion = "7.0.4", gradleVersion = "7.3") {

    @Test
    fun `does not break when there is a MR-JAR dependency with unsupported java version`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
            }

            sentry.tracingInstrumentation.enabled = true
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleDebug")
            .build()

        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    @Test
    fun `shows a warning when there is a signed MR-JAR dependency`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation("org.bouncycastle:bcprov-jdk15on:1.63")
            }

            sentry.tracingInstrumentation.enabled = true
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleDebug")
            .build()

        assertTrue { "Please update to AGP >= 7.1.2" in result.output }
        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    override val additionalRootProjectConfig: String = ""
}
