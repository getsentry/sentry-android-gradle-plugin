package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginCheckAndroidSdkTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  @Test
  fun `when tracingInstrumentation is disabled does not check sentry-android sdk state`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            sentry.tracingInstrumentation.enabled = false

            ${captureSdkState()}
            """
        .trimIndent()
    )

    // we query the SdkStateHolder intentionally so the build fails, which confirms that the
    // service was not registered
    val result = runner.appendArguments("app:tasks").buildAndFail()
    assertTrue {
      result.output.contains(
        Regex(
          """[BuildServiceRegistration with name 'io.sentry.android.gradle.services.SentryModulesService_(\w*)' not found]"""
        )
      )
    }
  }

  @Test
  fun `when tracingInstrumentation is enabled checks sentry-android sdk state`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            sentry.tracingInstrumentation.enabled = true
            sentry.autoInstallation.enabled = false
            sentry.includeProguardMapping = false

            ${captureSdkState()}
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug").build()
    assertTrue { "SENTRY MODULES: [:]" in result.output }
  }

  @Test
  fun `respects variant configuration`() {
    appBuildFile.appendText(
      // language=Groovy
      """
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
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug").build()

    assertTrue {
      "SENTRY MODULES: [io.sentry:sentry-android:5.4.0, " +
        "io.sentry:sentry-android-core:5.4.0, " +
        "io.sentry:sentry:5.4.0, " +
        "io.sentry:sentry-android-ndk:5.4.0]" in result.output
    }
  }

  private fun captureSdkState(): String =
    // language=Groovy
    """
        import io.sentry.android.gradle.autoinstall.BuildFinishedListenerService
        import io.sentry.android.gradle.util.*
        import io.sentry.android.gradle.services.*

        BuildFinishedListenerService.@Companion.getInstance(project.gradle).onClose {
            println(
                "SENTRY MODULES: " +
                    BuildServicesKt
                        .getBuildService(project.gradle.sharedServices, SentryModulesService.class)
                        .get().sentryModules
            )
        }
        """
      .trimIndent()
}
