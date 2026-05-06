package io.sentry.android.gradle

import io.sentry.android.gradle.sourcecontext.registerSentrySourceElements
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SentrySettingsPlugin : Plugin<Settings> {

  override fun apply(settings: Settings) {
    settings.gradle.beforeProject { project ->
      project.pluginManager.withPlugin("com.android.library") {
        registerSentrySourceElements(project)
      }
      project.pluginManager.withPlugin("java-library") { registerSentrySourceElements(project) }
    }
  }
}
