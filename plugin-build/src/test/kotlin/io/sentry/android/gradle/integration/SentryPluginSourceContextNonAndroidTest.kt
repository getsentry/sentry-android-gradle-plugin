package io.sentry.android.gradle.integration

import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.verifySourceBundleContents
import io.sentry.android.gradle.withDummyCustomFile
import io.sentry.android.gradle.withDummyJavaFile
import io.sentry.android.gradle.withDummyKtFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Test

class SentryPluginSourceContextNonAndroidTest :
    BaseSentryNonAndroidPluginTest(GradleVersion.current().version) {

    @Test
    fun `skips bundle and upload tasks if no sources`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "org.jetbrains.kotlin.jvm"
              id "io.sentry.jvm.gradle"
            }

            sentry {
              includeSourceContext = true
            }
            """.trimIndent()
        )
        val result = runner
            .appendArguments("app:assemble")
            .build()

        assertEquals(
            result.task(":app:sentryUploadSourceBundleJava")?.outcome,
            SKIPPED
        )
        assertEquals(
            result.task(":app:sentryBundleSourcesJava")?.outcome,
            SKIPPED
        )
        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    @Test
    fun `skips bundle and upload tasks if disabled`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "org.jetbrains.kotlin.jvm"
              id "io.sentry.jvm.gradle"
            }

            sentry {
              includeSourceContext = false
            }
            """.trimIndent()
        )

        sentryPropertiesFile.writeText("")

        testProjectDir.withDummyKtFile()
        testProjectDir.withDummyJavaFile()

        val result = runner
            .appendArguments("app:assemble")
            .build()

        assertEquals(
            result.task(":app:sentryUploadSourceBundleJava")?.outcome,
            SKIPPED
        )
        assertEquals(
            result.task(":app:sentryBundleSourcesJava")?.outcome,
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
              id "org.jetbrains.kotlin.jvm"
              id "io.sentry.jvm.gradle"
            }

            sentry {
              debug = true
              includeSourceContext = true
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              additionalSourceDirsForSourceContext = ["src/custom/kotlin"]
              org = "sentry-sdks"
              projectName = "sentry-android"
              url = "https://some-host.sentry.io"
            }
            """.trimIndent()
        )

        sentryPropertiesFile.writeText("")

        val ktContents = testProjectDir.withDummyKtFile()
        val javaContents = testProjectDir.withDummyJavaFile()
        val customContents = testProjectDir.withDummyCustomFile()

        val result = runner
            .appendArguments("app:assemble")
            .build()
        assertTrue { "\"--org\" \"sentry-sdks\"" in result.output }
        assertTrue { "\"--project\" \"sentry-android\"" in result.output }
        assertTrue { "\"--url\" \"https://some-host.sentry.io\"" in result.output }
        assertTrue { "BUILD SUCCESSFUL" in result.output }

        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/Example.jvm",
            ktContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/TestJava.jvm",
            javaContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/io/other/TestCustom.jvm",
            customContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
    }

    @Test
    fun `respects configuration cache`() {
        assumeThat(
            "SentryExternalDependenciesReportTask only supports " +
                "configuration cache from Gradle 7.5 onwards",
            GradleVersions.CURRENT >= GradleVersions.VERSION_7_5,
            `is`(true)
        )
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "java"
              id "io.sentry.jvm.gradle"
            }

            sentry {
              debug = true
              includeSourceContext = true
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              org = "sentry-sdks"
              projectName = "sentry-android"
            }
            """.trimIndent()
        )

        sentryPropertiesFile.writeText("")

        val javaContents = testProjectDir.withDummyJavaFile()

        val result = runner
            .appendArguments("app:assemble")
            .appendArguments("--configuration-cache")
            .build()

        assertTrue { "Configuration cache entry stored." in result.output }
        assertTrue { "BUILD SUCCESSFUL" in result.output }

        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/TestJava.jvm",
            javaContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
    }
}
