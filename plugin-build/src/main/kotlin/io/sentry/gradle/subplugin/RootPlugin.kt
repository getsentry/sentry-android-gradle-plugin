package io.sentry.gradle.subplugin

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.gradle.SENTRY_ORG_PARAMETER
import io.sentry.gradle.SENTRY_PROJECT_PARAMETER
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.StopExecutionException
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal

internal class RootPlugin(private val project: Project) {
  init {
    check(project == project.rootProject) {
      "This plugin must only be applied to the root project. Was ${project.path}."
    }
  }

  fun apply(buildEvents: BuildEventListenerRegistryInternal) =
    project.run {
      subprojects { p ->
        val extraProperties = p.extensions.getByName("ext") as ExtraPropertiesExtension
        val sentryOrgParameter =
          runCatching { extraProperties.get(SENTRY_ORG_PARAMETER).toString() }.getOrNull()
        val sentryProjectParameter =
          runCatching { extraProperties.get(SENTRY_PROJECT_PARAMETER).toString() }.getOrNull()

        p.pluginManager.withPlugin("com.android.library") {
          val sentryExtension = SentryPluginExtension.of(p)
          AndroidLibSubplugin(p).apply()
        }
        p.pluginManager.withPlugin("com.android.application") {
          val sentryExtension = SentryPluginExtension.of(p)
          AndroidAppSubplugin(p, sentryExtension)
            .apply(buildEvents, sentryOrgParameter, sentryProjectParameter)
        }

        // this has to be in afterEvaluate, else it might be too early to detect the deprecated
        // plugin
        p.afterEvaluate {
          p.pluginManager.withPlugin("com.android.application") {
            if (p.plugins.hasPlugin("io.sentry.android.gradle")) {
              throw StopExecutionException(
                "Using 'io.sentry.gradle' and 'io.sentry.android.gradle' plugins together is not supported."
              )
            }
          }
        }
      }
    }
}
