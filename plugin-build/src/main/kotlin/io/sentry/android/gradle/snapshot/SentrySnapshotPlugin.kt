package io.sentry.android.gradle.snapshot

import io.sentry.android.gradle.extensions.SnapshotsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Standalone plugin for automatic Compose @Preview snapshot testing via Paparazzi. */
class SentrySnapshotPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension =
      project.extensions.create("sentrySnapshots", SnapshotsExtension::class.java)

    project.afterEvaluate { configureIfReady(project, extension) }
  }

  private fun configureIfReady(project: Project, extension: SnapshotsExtension) {
    check(project.plugins.hasPlugin("app.cash.paparazzi")) {
      "Sentry Snapshot plugin requires 'app.cash.paparazzi' to be applied."
    }
    SnapshotTaskConfigurator.configure(project, extension)
  }
}
