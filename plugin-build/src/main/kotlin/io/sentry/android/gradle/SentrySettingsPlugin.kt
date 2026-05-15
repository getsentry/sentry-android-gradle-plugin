package io.sentry.android.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SentrySettingsPlugin : Plugin<Settings> {

  override fun apply(settings: Settings) {
    settings.gradle.beforeProject { project ->
      project.pluginManager.withPlugin("com.android.library") {
        project.pluginManager.apply("io.sentry.android.gradle")
      }
      project.pluginManager.withPlugin("java-library") {
        project.pluginManager.apply("io.sentry.android.gradle")
      }
    }
  }
}
