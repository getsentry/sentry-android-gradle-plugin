package io.sentry.android.gradle

import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtraPropertiesExtension

class SentrySettingsPlugin : Plugin<Settings> {

  override fun apply(settings: Settings) {
    val extension = settings.extensions.create("sentry", SentryPluginExtension::class.java)

    // Stash the extension in Gradle's ExtraPropertiesExtension so the project-level
    // plugin can read it — settings and project scopes are otherwise isolated.
    settings.gradle.extensions
      .getByType(ExtraPropertiesExtension::class.java)
      .set(SENTRY_SETTINGS_EXTENSION_KEY, extension)

    settings.gradle.beforeProject { project ->
      project.pluginManager.withPlugin("com.android.library") {
        project.pluginManager.apply("io.sentry.android.gradle")
      }
      project.pluginManager.withPlugin("java-library") {
        project.pluginManager.apply("io.sentry.jvm.gradle")
      }
    }
  }

  companion object {
    const val SENTRY_SETTINGS_EXTENSION_KEY = "io.sentry.settings.extension"
  }
}
