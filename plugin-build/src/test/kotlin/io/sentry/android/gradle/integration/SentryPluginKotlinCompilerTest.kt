package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.withDummyComposeFile
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginKotlinCompilerTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  override val additionalBuildClasspath: String =
    """
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0"
        classpath "org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.0"
        """
      .trimIndent()

  @Test
  fun `does not break for simple compose apps`() {
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "org.jetbrains.kotlin.android"
              id "org.jetbrains.kotlin.plugin.compose"
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
              compileOptions {
                sourceCompatibility JavaVersion.VERSION_17
                targetCompatibility JavaVersion.VERSION_17
              }
            }

            kotlin {
              jvmToolchain(17)
            }

            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:2.3.0"
                implementation "io.sentry:sentry-compose-android:${BuildConfig.SdkVersion}"

                implementation 'androidx.compose.ui:ui:1.7.0'
                implementation 'androidx.compose.ui:ui-tooling:1.7.0'
                implementation 'androidx.compose.foundation:foundation:1.7.0'
                implementation 'androidx.activity:activity-compose:1.9.0'
            }

            sentry {
              autoUploadProguardMapping = false
            }
            """
        .trimIndent()
    )

    testProjectDir.withDummyComposeFile()

    val result =
      runner
        .appendArguments("app:assembleRelease")
        // AGP 9.+ adds a stdlib dependency which we don't want to have for deterministic output
        .appendArguments("-Pandroid.builtInKotlin=false")
        .appendArguments("-Pandroid.newDsl=false")
        .build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
  }
}
