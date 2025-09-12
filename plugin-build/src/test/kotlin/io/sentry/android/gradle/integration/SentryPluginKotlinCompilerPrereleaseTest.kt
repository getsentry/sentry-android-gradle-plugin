package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.withDummyComposeFile
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginKotlinCompilerPrereleaseTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  override val additionalBuildClasspath: String =
    """
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20-RC"
        """
      .trimIndent()

  @Test
  fun `does not break for kotlin prereleases`() {
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

                freeCompilerArgs += [
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.8.20-RC"
                ]
              }
            }
            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.20-RC"
                implementation "io.sentry:sentry-compose-android:${BuildConfig.SdkVersion}"

                implementation 'androidx.compose.ui:ui:1.4.0'
                implementation 'androidx.compose.ui:ui-tooling:1.4.0'
                implementation 'androidx.compose.foundation:foundation:1.4.0'
                implementation 'androidx.activity:activity-compose:1.7.0'
            }

            sentry {
              autoUploadProguardMapping = false
            }
            """
        .trimIndent()
    )

    testProjectDir.withDummyComposeFile()

    val result = runner.appendArguments("app:assembleRelease").build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
  }
}
