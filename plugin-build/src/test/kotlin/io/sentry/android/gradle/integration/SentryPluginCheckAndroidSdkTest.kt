package io.sentry.android.gradle.integration

import com.google.common.truth.Truth.assertThat
import io.sentry.BuildConfig
import io.sentry.android.gradle.READ_MODULES_PREFIX
import io.sentry.android.gradle.readSentryModules
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginCheckAndroidSdkTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  @Test
  fun `when instrumentation is disabled does not read the sentry modules`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            sentry.tracingInstrumentation.enabled = false
            sentry.runtimeOptimizations.enabled = false
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug", "--info").build()

    assertThat(result.output).doesNotContain(READ_MODULES_PREFIX)
  }

  @Test
  fun `when tracingInstrumentation is enabled reads the sentry modules`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            sentry.tracingInstrumentation.enabled = true
            sentry.autoInstallation.enabled = false
            sentry.includeProguardMapping = false
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug", "--info").build()

    assertThat(result.output.readSentryModules()).isEqualTo("{}")
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
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug", "--info").build()

    assertThat(result.output.readSentryModules())
      .isEqualTo(
        "{io.sentry:sentry=5.4.0, " +
          "io.sentry:sentry-android=5.4.0, " +
          "io.sentry:sentry-android-core=5.4.0, " +
          "io.sentry:sentry-android-ndk=5.4.0}"
      )
  }
}
