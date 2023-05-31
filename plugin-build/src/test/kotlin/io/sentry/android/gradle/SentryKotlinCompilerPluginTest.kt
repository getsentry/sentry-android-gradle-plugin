package io.sentry.android.gradle

import io.sentry.BuildConfig
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentryKotlinCompilerPluginTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    override val additionalBuildClasspath: String =
        """
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20"
        """.trimIndent()

    @Test
    fun `does not break for simple compose apps`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "kotlin-android"
              id "io.sentry.android.gradle"
              id "io.sentry.kotlin.compiler.gradle"
            }

            android {
              namespace 'com.example'
              buildTypes {
                release {
                  minifyEnabled = true
                }
              }
              buildFeatures {
                compose true
              }
              composeOptions {
                kotlinCompilerExtensionVersion = "1.4.6"
              }
              kotlinOptions {
                jvmTarget = "1.8"
              }
            }
            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.20"
                implementation "io.sentry:sentry-compose-android:${BuildConfig.SdkVersion}"

                implementation 'androidx.compose.ui:ui:1.4.0'
                implementation 'androidx.compose.ui:ui-tooling:1.4.0'
                implementation 'androidx.compose.foundation:foundation:1.4.0'
                implementation 'androidx.activity:activity-compose:1.7.0'
            }

            sentry {
              autoUploadProguardMapping = false
            }
            """.trimIndent()
        )

        testProjectDir.withDummyComposeFile()

        val result = runner
            .appendArguments("app:assembleRelease")
            .build()

        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }
}
