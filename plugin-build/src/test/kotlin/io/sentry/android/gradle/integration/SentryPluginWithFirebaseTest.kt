package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginWithFirebaseTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

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
              namespace 'com.example'

              buildTypes {
                release {
                  proguardFiles 'proguard-rules.pro'
                  minifyEnabled = true
                }
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-android-core:5.6.0'
              implementation 'androidx.work:work-runtime:2.5.0'
              implementation 'com.google.firebase:firebase-perf-ktx:20.4.1'
            }

            sentry {
              autoUploadProguardMapping = false
            }
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleRelease").build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
  }

  override val additionalBuildClasspath: String =
    """
        classpath 'com.google.firebase:perf-plugin:1.4.2'
        """
      .trimIndent()
}
