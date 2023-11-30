package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.verifyDependenciesReportAndroid
import kotlin.test.assertEquals
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
        appBuildFile.appendText(
            // language=Groovy
            """

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
              implementation project(':module') // multi-module project dependency
              implementation ':asm-9.2' // flat jar
            }
            """.trimIndent()
        )
        print(appBuildFile.readText())
        runner.appendArguments(":app:assembleDebug")
            .appendArguments("--configuration-cache")

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
            "{io.sentry:sentry-android-core=6.30.0, io.sentry:sentry=6.30.0, io.sentry:sentry-android-okhttp=6.30.0, io.sentry:sentry-android-sqlite=6.30.0}",
            readSentryModules
        )
        /* ktlint-enable max-line-length */
    }
}
