package io.sentry.android.gradle

import io.sentry.android.gradle.integration.BaseSentryPluginTest
import kotlin.test.assertTrue
import org.junit.Test

class SentryPluginMRJarTest :
  BaseSentryPluginTest(androidGradlePluginVersion = "7.4.0", gradleVersion = "7.6.4") {

  @Test
  fun `does not break when there is a MR-JAR dependency with unsupported java version`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            dependencies {
              implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
            }

            sentry.tracingInstrumentation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug").build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
  }

  override val additionalRootProjectConfig: String = ""
}
