package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.verifySourceBundleContents
import io.sentry.android.gradle.withDummyComposeFile
import io.sentry.android.gradle.withDummyCustomFile
import io.sentry.android.gradle.withDummyJavaFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginSourceContextTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

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
