package io.sentry.android.gradle

import com.android.build.gradle.internal.cxx.json.readJsonFile
import io.sentry.android.gradle.util.SentryAndroidSdkState
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginCheckAndroidSdkTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun `when tracingInstrumentation is disabled does not check sentry-android sdk state`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            sentry.tracingInstrumentation.enabled = false
            """.trimIndent()
        )

        runner
            .appendArguments("app:tasks")
            .build()
        val sdkStateFile = testProjectDir.root
            .resolve("app/build/${SentryPlugin.buildSdkStateFilePath("debug")}")
        assertFalse { sdkStateFile.exists() }
    }

    @Test
    fun `when tracingInstrumentation is enabled checks sentry-android sdk state`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            sentry.tracingInstrumentation.enabled = true
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        runner
            .appendArguments("app:tasks")
            .build()
        val sdkStateFile = testProjectDir.root
            .resolve("app/build/${SentryPlugin.buildSdkStateFilePath("debug")}")
        assertTrue {
            sdkStateFile.exists() &&
                readJsonFile(
                    sdkStateFile,
                    SentryAndroidSdkState::class.java
                ) == SentryAndroidSdkState.MISSING
        }
    }

    @Test
    fun `respects variant configuration`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            sentry {
              tracingInstrumentation.enabled = true
              includeProguardMapping = false
            }

            configurations {
              debugImplementation
              releaseImplementation
            }

            dependencies {
              debugImplementation 'io.sentry:sentry-android:5.4.0'
              releaseImplementation 'io.sentry:sentry-android:5.5.0'
            }
            """.trimIndent()
        )

        runner
            .appendArguments("app:tasks")
            .build()

        val sdkStateFileDebug = testProjectDir.root
            .resolve("app/build/${SentryPlugin.buildSdkStateFilePath("debug")}")
        val sdkStateFileRelease = testProjectDir.root
            .resolve("app/build/${SentryPlugin.buildSdkStateFilePath("release")}")

        assertTrue {
            sdkStateFileDebug.exists() &&
                readJsonFile(
                    sdkStateFileDebug,
                    SentryAndroidSdkState::class.java
                ) == SentryAndroidSdkState.PERFORMANCE
        }
        assertTrue {
            sdkStateFileRelease.exists() &&
                readJsonFile(
                    sdkStateFileRelease,
                    SentryAndroidSdkState::class.java
                ) == SentryAndroidSdkState.FILE_IO
        }
    }
}
