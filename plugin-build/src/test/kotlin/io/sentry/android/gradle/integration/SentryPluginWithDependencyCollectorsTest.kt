package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginWithDependencyCollectorsTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

    @Test
    fun `does not break when there are plugins that collect dependencies applied`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
              id "com.mikepenz.aboutlibraries.plugin"
              id "com.google.android.gms.oss-licenses-plugin"
            }

            android {
              namespace 'com.example'

              buildTypes {
                release {
                  minifyEnabled true
                }
              }
            }

            dependencies {
              implementation 'androidx.compose.runtime:runtime:1.3.0'
              implementation 'androidx.compose.ui:ui:1.3.0'
            }

            sentry {
              autoUploadProguardMapping = false
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleRelease")
            .build()

        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
    }

    override val additionalBuildClasspath: String =
        """
        classpath 'com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:10.6.1'
        classpath 'com.google.android.gms:oss-licenses-plugin:0.10.5'
        """.trimIndent()
}
