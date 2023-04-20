package io.sentry.android.gradle

import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

class SentryKotlinCompilerPluginTest :
    BaseSentryPluginTest(androidGradlePluginVersion = "7.4.0", gradleVersion = "8.0") {

    override val additionalRootProjectConfig: String
        get() = """

        """.trimIndent()

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
              id "io.sentry.sentry-kotlin-compiler-gradle-plugin"
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
                kotlinCompilerExtensionVersion = "1.4.4"
              }
              kotlinOptions {
                jvmTarget = "1.8"
              }
            }
            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.20"
                implementation "io.sentry:sentry-compose-android:6.18.0-mah-dev-026"

                implementation 'androidx.compose.ui:ui:1.4.0'
                implementation 'androidx.compose.ui:ui-tooling:1.4.0'
                implementation 'androidx.compose.foundation:foundation:1.4.0'
                implementation 'androidx.activity:activity-compose:1.7.0'
            }

            sentry {
              autoUploadProguardMapping = false
            }

            sentryKotlinCompiler {
              enabled = true
            }
            """.trimIndent()
        )

        val sourceFile =
            File(testProjectDir.newFolder("app/src/main/java/com/example/"), "Example.kt")

        sourceFile.writeText(
            // language=kotlin
            """
            package com.example

            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.text.BasicText

            @Composable
            fun FancyButton() {
                BasicText("Hello World")
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleRelease")
            .build()

        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }
}
