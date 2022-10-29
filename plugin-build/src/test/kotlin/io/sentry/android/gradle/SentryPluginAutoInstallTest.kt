package io.sentry.android.gradle

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentryPluginAutoInstallTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

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

        val result = runner
            .appendArguments("app:dependencies")
            .appendArguments("--configuration")
            .appendArguments("debugRuntimeClasspath")
            .build()
        assertTrue {
            "io.sentry:sentry-android:$SENTRY_SDK_VERSION" in result.output
        }
    }

    @Test
    fun `adds integrations`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              // sentry-android shouldn't be installed, since sentry-android-core is present
              implementation 'io.sentry:sentry-android-core:5.1.0'
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'androidx.fragment:fragment:1.3.5'
              // our plugin shouldn't install okhttp, since it's a direct dep
              implementation 'io.sentry:sentry-android-okhttp:5.4.0'
            }

            sentry {
              includeProguardMapping = false
              autoInstallation.enabled = true
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:dependencies")
            .appendArguments("--configuration")
            .appendArguments("debugRuntimeClasspath")
            .build()
        assertFalse { "io.sentry:sentry-android:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-timber:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-fragment:5.1.0" in result.output }
        assertFalse { "io.sentry:sentry-android-okhttp:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-okhttp:5.4.0" in result.output }
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

        val result = runner
            .appendArguments("app:dependencies")
            .appendArguments("--configuration")
            .appendArguments("debugRuntimeClasspath")
            .build()
        assertFalse { "io.sentry:sentry-android:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-timber:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-fragment:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-okhttp:$SENTRY_SDK_VERSION" in result.output }
    }

    @Test
    fun `uses user-provided sentryVersion when sentry-android is not available in direct deps`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'com.squareup.okhttp3:okhttp:4.9.2'
              // the fragment integration should stay as it is, the version shouldn't be overridden
              implementation 'io.sentry:sentry-android-fragment:5.4.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "5.1.2"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:dependencies")
            .appendArguments("--configuration")
            .appendArguments("debugRuntimeClasspath")
            .build()
        assertTrue { "io.sentry:sentry-android:5.1.2" in result.output }
        assertTrue { "io.sentry:sentry-android-timber:5.1.2" in result.output }
        assertTrue { "io.sentry:sentry-android-okhttp:5.1.2" in result.output }
        assertTrue { "io.sentry:sentry-android-fragment:5.4.0" in result.output }
    }
}
