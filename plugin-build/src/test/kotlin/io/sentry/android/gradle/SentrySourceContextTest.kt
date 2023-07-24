package io.sentry.android.gradle

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentrySourceContextTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun `skips bundle and upload tasks if no sources`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'

              buildFeatures {
                buildConfig false
              }
            }

            sentry {
              includeSourceContext = true
            }
            """.trimIndent()
        )
        val result = runner
            .appendArguments("app:assembleRelease")
            .build()

        assertEquals(
            result.task(":app:sentryUploadSourceBundleRelease")?.outcome,
            SKIPPED
        )
        assertEquals(
            result.task(":app:sentryBundleSourcesRelease")?.outcome,
            SKIPPED
        )
        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    @Test
    fun `bundles source context`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'

              buildFeatures {
                buildConfig false
              }
            }

            sentry {
              debug = true
              includeSourceContext = true
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              additionalSourceDirsForSourceContext = ["src/custom/kotlin"]
              org = "sentry-sdks"
              projectName = "sentry-android"
            }
            """.trimIndent()
        )

        sentryPropertiesFile.writeText("")

        val ktContents = testProjectDir.withDummyComposeFile()
        val javaContents = testProjectDir.withDummyJavaFile()
        val customContents = testProjectDir.withDummyCustomFile()

        val result = runner
            .appendArguments("app:assembleRelease")
            .build()

        assertTrue { "\"--org\" \"sentry-sdks\"" in result.output }
        assertTrue { "\"--project\" \"sentry-android\"" in result.output }
        assertTrue { "BUILD SUCCESSFUL" in result.output }

        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/Example.jvm",
            ktContents
        )
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/TestJava.jvm",
            javaContents
        )
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/io/other/TestCustom.jvm",
            customContents
        )
    }
}
