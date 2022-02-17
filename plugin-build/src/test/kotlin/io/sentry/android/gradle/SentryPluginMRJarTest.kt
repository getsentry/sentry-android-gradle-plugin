package io.sentry.android.gradle

import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentryPluginMRJarTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun `does not break when there is a MR-JAR dependency with unsupported java version`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            dependencies {
              implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
            }

            sentry.tracingInstrumentation.enabled = true
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleDebug")
            .build()

        print(result.output)

        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    override val additionalRootProjectConfig: String = ""
}
