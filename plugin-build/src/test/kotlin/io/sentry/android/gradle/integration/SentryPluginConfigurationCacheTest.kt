package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryCliProvider
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.verifyDependenciesReportAndroid
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Test

class SentryPluginConfigurationCacheTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

    @Test
    fun `dependency collector task respects configuration cache`() {
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
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            repositories {
                flatDir {
                    dir('../libs')
                }
            }

            android {
              namespace 'com.example'
            }

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
              implementation project(':module') // multi-module project dependency
              implementation ':asm-9.2' // flat jar
            }

            sentry {
              autoUploadProguardMapping = false
              autoInstallation.enabled = false
              telemetry = false
            }
            """.trimIndent()
        )
        runner.appendArguments(":app:assembleDebug")
            .appendArguments("--configuration-cache")
            .appendArguments("--info")

        val output = runner.build().output
        val deps = verifyDependenciesReportAndroid(testProjectDir.root)
        assertEquals(
            """
            com.squareup.okhttp3:okhttp:3.14.9
            com.squareup.okio:okio:1.17.2
            """.trimIndent(),
            deps,
            "$deps\ndo not match expected value"
        )
        assertTrue { "Configuration cache entry stored." in output }

        val outputWithConfigCache = runner.build().output
        assertTrue { "Configuration cache entry reused." in outputWithConfigCache }
    }

    @Test
    fun `SentryModulesService is not discarded at configuration phase`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
            }

            dependencies {
              implementation 'io.sentry:sentry-android-core:6.30.0'
              implementation 'io.sentry:sentry-android-okhttp:6.30.0'
              implementation 'androidx.work:work-runtime:2.5.0'
            }

            sentry {
              autoUploadProguardMapping = false
              autoInstallation.enabled = false
              telemetry = false
            }
            """.trimIndent()
        )

        runner.appendArguments(":app:assembleDebug")
            .appendArguments("--configuration-cache")
            .appendArguments("--info")

        val output = runner.build().output
        val readSentryModules = output
            .lines()
            .find { it.startsWith("[sentry] Read sentry modules:") }
            ?.substringAfter("[sentry] Read sentry modules:")
            ?.trim()
        /* ktlint-disable max-line-length */
        assertEquals(
            "{io.sentry:sentry-android-core=6.30.0, io.sentry:sentry=6.30.0, io.sentry:sentry-android-okhttp=6.30.0}",
            readSentryModules
        )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun `works well with configuration cache`() {
        // configuration cache doesn't seem to work well on older Gradle/AGP combinations
        // producing the following output:
        //
        // 0 problems were found storing the configuration cache.
        // Configuration cache entry discarded
        //
        // so we only run this test on supported versions
        assumeThat(
            "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
            SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
                GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
            `is`(true)
        )

        val runner = runner.withArguments(
            "--configuration-cache",
            "--build-cache",
            ":app:assembleDebug"
        )

        val run0 = runner.build()
        assertFalse(
            "Reusing configuration cache." in run0.output ||
                "Configuration cache entry reused." in run0.output,
            run0.output
        )

        val run1 = runner.build()
        assertTrue(
            "Reusing configuration cache." in run1.output ||
                "Configuration cache entry reused." in run1.output,
            run1.output
        )
    }

    @Test
    fun `sentry-cli is recovered when deleted during runs and configuration cache is active`() {
        // configuration cache doesn't seem to work well on older Gradle/AGP combinations
        // producing the following output:
        //
        // 0 problems were found storing the configuration cache.
        // Configuration cache entry discarded
        //
        // so we only run this test on supported versions
        assumeThat(
            "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
            SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
                GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
            `is`(true)
        )

        val runner = runner.withArguments(
            "--configuration-cache",
            "--build-cache",
            ":app:assembleDebug"
        )

        val run0 = runner.build()
        assertFalse(
            "Reusing configuration cache." in run0.output ||
                "Configuration cache entry reused." in run0.output,
            run0.output
        )

        val cliPath = SentryCliProvider.getCliResourcesExtractionPath(
            File(runner.projectDir, "build")
        )

        // On Gradle >= 8, the whole build folder is wiped anyway
        if (cliPath.exists()) {
            cliPath.delete()
        }

        // then it should be recovered on the next run
        val run1 = runner.build()
        assertTrue(
            "Reusing configuration cache." in run1.output ||
                "Configuration cache entry reused." in run1.output,
            run1.output
        )
        assertTrue(run1.output) { "BUILD SUCCESSFUL" in run1.output }
    }

    @Test
    fun `sentry-cli is recovered when clean is executed before assemble`() {
        // configuration cache doesn't seem to work well on older Gradle/AGP combinations
        // producing the following output:
        //
        // 0 problems were found storing the configuration cache.
        // Configuration cache entry discarded
        //
        // so we only run this test on supported versions
        assumeThat(
            "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
            SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
                GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
            `is`(true)
        )

        val runner = runner.withArguments(
            "--configuration-cache",
            "--build-cache",
            ":app:clean",
            ":app:assembleRelease"
        )

        val run = runner.build()
        assertTrue(run.output) { "BUILD SUCCESSFUL" in run.output }
    }
}
