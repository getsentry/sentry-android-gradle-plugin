package io.sentry.android.gradle

import kotlin.test.assertTrue
import org.junit.Test

class SentryPluginWithPlayCoreTest :
    BaseSentryPluginTest(androidGradlePluginVersion = "7.1.2", gradleVersion = "7.4") {

    @Test
    fun `does not break when there is a play-core obfuscated dependency`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            dependencies {
              implementation 'io.sentry:sentry-android-core:5.6.0'
              implementation 'com.google.android.play:core-ktx:1.8.1'
            }
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
