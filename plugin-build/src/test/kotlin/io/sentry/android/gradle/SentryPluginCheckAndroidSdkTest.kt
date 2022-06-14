package io.sentry.android.gradle

import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginCheckAndroidSdkTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun `when tracingInstrumentation is disabled does not check sentry-android sdk state`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            sentry.tracingInstrumentation.enabled = false

            ${captureSdkState()}
            """.trimIndent()
        )

        // we query the SdkStateHolder intentionally so the build fails, which confirms that the
        // service was not registered
        val result = runner
            .appendArguments("app:tasks")
            .buildAndFail()
        /* ktlint-disable max-line-length */
        assertTrue {
            result.output.contains(
                Regex(
                    """[BuildServiceRegistration with name 'io.sentry.android.gradle.services.SentryModulesService_(\w*)' not found]"""
                )
            )
        }
        /* ktlint-enable max-line-length */
    }

    @Test
    fun `when tracingInstrumentation is enabled checks sentry-android sdk state`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            sentry.tracingInstrumentation.enabled = true
            sentry.autoInstallation.enabled = false
            sentry.includeProguardMapping = false

            ${captureSdkState()}
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleDebug")
            .build()
        assertTrue {
            "SENTRY MODULES: [:]" in result.output
        }
    }

    @Test
    fun `respects variant configuration`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            sentry {
              tracingInstrumentation.enabled = true
              autoInstallation.enabled = false
              includeProguardMapping = false
            }

            configurations {
              debugImplementation
              releaseImplementation
            }

            dependencies {
              debugImplementation 'io.sentry:sentry-android:5.4.0'
              releaseImplementation 'io.sentry:sentry-android:5.5.0'
            }

            ${captureSdkState()}
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:assembleDebug")
            .build()

        assertTrue {
            "SENTRY MODULES: [sentry-android:5.4.0, sentry-android-core:5.4.0, " +
                "sentry:5.4.0, sentry-android-ndk:5.4.0]" in result.output
        }
    }

    private fun captureSdkState(): String =
        // language=Groovy
        """
        import io.sentry.android.gradle.util.*
        import io.sentry.android.gradle.services.*
        project.gradle.buildFinished {
          println(
            "SENTRY MODULES: " + BuildServicesKt
              .getBuildService(project.gradle.sharedServices, SentryModulesService.class)
              .get().modules
          )
        }
        """.trimIndent()
}
