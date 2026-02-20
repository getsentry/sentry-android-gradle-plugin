package io.sentry.android.gradle.snapshot

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.extensions.SnapshotExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.LoggerFactory

/** Standalone plugin for automatic Compose @Preview snapshot testing via Paparazzi. */
class SentrySnapshotPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension =
      project.extensions.findByType(SentryPluginExtension::class.java)?.snapshots
        ?: project.extensions.create("sentrySnapshots", SnapshotExtension::class.java)

    if (project.state.executed) {
      // Applied from SentryPlugin.afterEvaluate — extension values are finalized
      configureIfReady(project, extension)
    } else {
      project.afterEvaluate { configureIfReady(project, extension) }
    }
  }

  private fun configureIfReady(project: Project, extension: SnapshotExtension) {
    if (!project.plugins.hasPlugin("app.cash.paparazzi")) {
      logger.warn(
        "Sentry Snapshot plugin requires 'app.cash.paparazzi' to be applied. Skipping configuration."
      )
      return
    }
    SnapshotTaskConfigurator.configure(project, extension)
  }

  companion object {
    private val logger by lazy { LoggerFactory.getLogger(SentrySnapshotPlugin::class.java) }
  }
}
