// ktlint-disable max-line-length
package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginAutoInstallTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

    @Test
    fun `adds sentry-android dependency`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            sentry {
              includeProguardMapping = false
              autoInstallation.enabled = true
            }
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertTrue {
            "io.sentry:sentry-android:$SENTRY_SDK_VERSION" in result.output
        }
    }

    @Test
    fun `adds integrations and overrides directly user-defined versions with what -core has`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              // sentry-android shouldn't be installed, since sentry-android-core is present
              implementation 'io.sentry:sentry-android-core:6.34.0'
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'androidx.fragment:fragment:1.3.5'
              // our plugin should override okhttp to 6.34.0
              implementation 'com.squareup.okhttp3:okhttp:4.9.2'
              implementation 'io.sentry:sentry-android-okhttp:6.31.0'
              // our plugin should override sqlite to 6.34.0
              implementation 'androidx.sqlite:sqlite:2.0.0'
              implementation 'io.sentry:sentry-android-sqlite:6.21.0'
            }

            sentry {
              includeProguardMapping = false
              autoInstallation.enabled = true
              includeDependenciesReport = false
            }
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertFalse { "io.sentry:sentry-android:6.34.0" in result.output }
        assertTrue { "io.sentry:sentry-android-timber:6.34.0" in result.output }
        assertTrue { "io.sentry:sentry-android-fragment:6.34.0" in result.output }
        assertTrue { "io.sentry:sentry-android-okhttp:6.31.0 -> 6.34.0" in result.output }
        assertTrue { "io.sentry:sentry-android-sqlite:6.21.0 -> 6.34.0" in result.output }
        assertFalse { "io.sentry:sentry-compose-android:6.34.0" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `does not do anything when autoinstall is disabled`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'androidx.fragment:fragment:1.3.5'
              implementation 'com.squareup.okhttp3:okhttp:4.9.2'
            }

            sentry.autoInstallation.enabled = false
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertFalse { "io.sentry:sentry-android:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-timber:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-fragment:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-okhttp:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-sqlite:$SENTRY_SDK_VERSION" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `uses user-provided sentryVersion when sentry-android is not available in direct deps`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'com.squareup.okhttp3:okhttp:4.9.2'
              implementation 'io.sentry:sentry-android-fragment:5.4.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.31.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-android:6.31.0" in result.output }
        assertTrue { "io.sentry:sentry-android-timber:6.31.0" in result.output }
        assertTrue { "io.sentry:sentry-android-okhttp:6.31.0" in result.output }
        assertTrue { "io.sentry:sentry-android-fragment:5.4.0 -> 6.31.0" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `compose is not added for lower sentry versions`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.compose.runtime:runtime:1.1.1'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.6.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-compose-android:6.6.0" in result.output }
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `compose is added with when sentry version 6_7_0 or above is used`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.compose.runtime:runtime:1.1.1'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.7.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-compose-android:6.7.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `sqlite is not added for lower sentry versions`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.sqlite:sqlite:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.20.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-android-sqlite:6.20.0" in result.output }
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `sqlite is added with when sentry version 6_21_0 or above is used`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.sqlite:sqlite:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.21.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-android-sqlite:6.21.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `warns about overriding user-defined sentry dependencies`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation project(':module')
            }
            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.21.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        moduleBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'io.sentry:sentry-android-core:6.0.0'
            }
            """.trimIndent()
        )

        val result = runner.appendArguments("app:assembleDebug").build()
        assertTrue { "WARNING: Version of 'io.sentry:sentry-android-core' was overridden from '6.0.0' to '6.21.0' by the Sentry Gradle plugin. If you want to use the older version, you can add `autoInstallation.sentryVersion.set(\"6.0.0\")` in the `sentry {}` plugin configuration block" in result.output }
    }

    private fun runListDependenciesTask() = runner
        .appendArguments("app:dependencies")
        .appendArguments("--configuration")
        .appendArguments("debugRuntimeClasspath")
        .build()
}
