package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryCliProvider
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.verifySourceBundleContents
import io.sentry.android.gradle.withDummyComposeFile
import io.sentry.android.gradle.withDummyCustomFile
import io.sentry.android.gradle.withDummyJavaFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
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
        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
    }

    @Test
    fun `generateBundleId and collectSources are up-to-date on subsequent builds`() {
        runner.appendArguments("app:assembleRelease")
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
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              org = "sentry-sdks"
              projectName = "sentry-android"
              url = "https://some-host.sentry.io"
            }
            """.trimIndent()
        )
        val firstBuild = runner.build()

        val subsequentBuild = runner.build()

        assertEquals(
            firstBuild.task(":app:generateSentryBundleIdRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            firstBuild.task(":app:sentryCollectSourcesRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            subsequentBuild.task(":app:generateSentryBundleIdRelease")?.outcome,
            UP_TO_DATE
        )

        assertEquals(
            subsequentBuild.task(":app:sentryCollectSourcesRelease")?.outcome,
            UP_TO_DATE
        )

        assertTrue(subsequentBuild.output) { "BUILD SUCCESSFUL" in subsequentBuild.output }
    }

    @Test
    fun `generateBundleId and collectSources are not up-to-date if sources change`() {
        runner.appendArguments("app:assembleRelease")
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
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              org = "sentry-sdks"
              projectName = "sentry-android"
              url = "https://some-host.sentry.io"
            }
            """.trimIndent()
        )
        val firstBuild = runner.build()

        testProjectDir.withDummyComposeFile()

        val subsequentBuild = runner.build()

        assertEquals(
            firstBuild.task(":app:generateSentryBundleIdRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            firstBuild.task(":app:sentryCollectSourcesRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            subsequentBuild.task(":app:generateSentryBundleIdRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            subsequentBuild.task(":app:sentryCollectSourcesRelease")?.outcome,
            SUCCESS
        )

        assertTrue(subsequentBuild.output) { "BUILD SUCCESSFUL" in subsequentBuild.output }
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
                buildConfig true
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
              url = "https://some-host.sentry.io"
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

        assertTrue(result.output) { "\"--org\" \"sentry-sdks\"" in result.output }
        assertTrue(result.output) { "\"--project\" \"sentry-android\"" in result.output }
        assertTrue(result.output) { "\"--url\" \"https://some-host.sentry.io\"" in result.output }
        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }

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
        // do not bundle build config
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/BuildConfig.jvm",
            ""
        )
    }

    @Test
    fun `respects configuration cache`() {
        assumeThat(
            "Sentry Source Context only supports " +
                "configuration cache from Gradle 8.0 onwards due to the bug in gradle " +
                "https://github.com/gradle/gradle/issues/19252",
            GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
            `is`(true)
        )

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
                buildConfig true
              }
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

        val ktContents = testProjectDir.withDummyComposeFile()

        val result = runner
            .appendArguments("app:assembleRelease")
            .appendArguments("--configuration-cache")
            .build()

        assertTrue(result.output) { "Configuration cache entry stored." in result.output }
        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }

        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/Example.jvm",
            ktContents
        )
        // do not bundle build config
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/BuildConfig.jvm",
            ""
        )
    }

    @Test
    fun `uploadSourceBundle task is up-to-date on subsequent builds`() {
        val sentryCli = SentryCliProvider.getSentryCliPath(
            File(""),
            File("build"),
            File("")
        )
        SentryCliProvider.maybeExtractFromResources(File("build"), sentryCli)

        sentryPropertiesFile.writeText("cli.executable=$sentryCli")

        runner.appendArguments("app:assembleRelease")
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
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              org = "sentry-sdks"
              projectName = "sentry-android"
            }
            """.trimIndent()
        )
        testProjectDir.withDummyComposeFile()

        val firstBuild = runner.build()

        val subsequentBuild = runner.build()

        assertEquals(
            firstBuild.task(":app:sentryUploadSourceBundleRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            subsequentBuild.task(":app:sentryUploadSourceBundleRelease")?.outcome,
            UP_TO_DATE
        )

        assertTrue(subsequentBuild.output) { "BUILD SUCCESSFUL" in subsequentBuild.output }
    }

    @Test
    fun `uploadSourceBundle task is not up-to-date on subsequent builds if cli path changes`() {
        val sentryCli = SentryCliProvider.getSentryCliPath(
            File(""),
            File("build"),
            File("")
        )
        SentryCliProvider.maybeExtractFromResources(File("build"), sentryCli)

        sentryPropertiesFile.writeText("cli.executable=$sentryCli")

        runner.appendArguments("app:assembleRelease")
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
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              org = "sentry-sdks"
              projectName = "sentry-android"
            }
            """.trimIndent()
        )
        testProjectDir.withDummyComposeFile()

        val firstBuild = runner.build()

        val tempDir = Files.createTempDirectory("sentry-test")
        val newCliPath = tempDir.resolve("sentry-cli")
        Files.copy(Path.of(sentryCli), tempDir.resolve("sentry-cli"))
        newCliPath.toFile().deleteOnExit()
        tempDir.toFile().deleteOnExit()

        sentryPropertiesFile.writeText("cli.executable=$newCliPath")

        val subsequentBuild = runner.build()

        assertEquals(
            firstBuild.task(":app:sentryUploadSourceBundleRelease")?.outcome,
            SUCCESS
        )

        assertEquals(
            subsequentBuild.task(":app:sentryUploadSourceBundleRelease")?.outcome,
            SUCCESS
        )

        assertTrue(subsequentBuild.output) { "BUILD SUCCESSFUL" in subsequentBuild.output }
    }
}
