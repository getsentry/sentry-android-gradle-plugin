package io.sentry.android.gradle

import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class SentryPluginWithFirebaseTest :
    BaseSentryPluginTest(androidGradlePluginVersion = "7.1.0", gradleVersion = "7.3.3") {

    @Test
    fun `does not break when there is a firebase-perf plugin applied`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
              id "com.google.firebase.firebase-perf"
            }

            android {
              buildTypes {
                release {
                  minifyEnabled = true
                }
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-android-core:5.6.0'
              implementation 'androidx.work:work-runtime:2.5.0'
            }

            sentry {
              autoUploadProguardMapping = false
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleRelease")
            .build()

        print(result.output)

        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    override val additionalBuildClasspath: String =
        """
        classpath 'com.google.firebase:perf-plugin:1.4.1'
        """.trimIndent()
}
