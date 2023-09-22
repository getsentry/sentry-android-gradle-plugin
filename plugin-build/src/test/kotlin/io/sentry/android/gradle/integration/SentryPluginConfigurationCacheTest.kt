package io.sentry.android.gradle.integration

import io.sentry.android.gradle.verifyDependenciesReportAndroid
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SentryPluginConfigurationCacheTest :
    BaseSentryPluginTest("7.3.0", "7.5") {

    @Test
    fun `respects configuration cache`() {
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
        runner.appendArguments(":app:assembleDebug")
            .appendArguments("--configuration-cache")

        val output = runner.build().output
        val deps = verifyDependenciesReportAndroid(testProjectDir.root)
        assertEquals(
            """
            com.squareup.okhttp3:okhttp:3.14.9
            com.squareup.okio:okio:1.17.2
            """.trimIndent(),
            deps
        )
        assertTrue { "Configuration cache entry stored." in output }

        val outputWithConfigCache = runner.build().output
        assertTrue { "Configuration cache entry reused." in outputWithConfigCache }
    }
}
