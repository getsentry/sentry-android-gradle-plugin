package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginWithMinifiedLibsTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  @Test
  fun `does not break when there is a minified jar dependency`() {
    appBuildFile.appendText(
      // language=Groovy
      """
            dependencies {
              implementation 'io.sentry:sentry-android-core:${BuildConfig.SdkVersion}'
              implementation 'com.google.android.play:core-ktx:1.8.1'
              implementation 'com.google.android.gms:play-services-vision:20.1.3'
              implementation 'com.google.android.gms:play-services-mlkit-text-recognition:18.0.0'
              implementation 'com.adcolony:sdk:4.7.1'
              implementation 'com.appboy:android-sdk-ui:19.0.0'
              implementation 'com.stripe:stripeterminal-internal-common:2.12.0'
              implementation 'com.synerise.sdk:synerise-mobile-sdk:4.8.0'
              implementation 'com.google.android.gms:play-services-mlkit-face-detection:17.1.0'
              implementation 'com.facebook.android:facebook-core:16.3.0'
            }

            sentry.tracingInstrumentation.forceInstrumentDependencies = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:assembleDebug").build()

    assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
  }

  override val additionalRootProjectConfig: String = ""
}
