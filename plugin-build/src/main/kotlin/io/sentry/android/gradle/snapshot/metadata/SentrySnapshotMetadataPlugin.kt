package io.sentry.android.gradle.snapshot.metadata

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class SentrySnapshotMetadataPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension =
      project.extensions.create(
        "sentrySnapshotMetadata",
        SentrySnapshotMetadataExtension::class.java,
        project.objects,
      )

    fun wireWithAndroid() {
      val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
      androidComponents.onVariants { variant ->
        ExportPreviewMetadataTask.register(project, extension, variant.name)
      }
    }

    project.pluginManager.withPlugin("com.android.library") { wireWithAndroid() }
    project.pluginManager.withPlugin("com.android.application") { wireWithAndroid() }

    project.afterEvaluate {
      val hasAndroid =
        project.pluginManager.hasPlugin("com.android.library") ||
          project.pluginManager.hasPlugin("com.android.application")
      if (!hasAndroid) {
        error(
          "'io.sentry.android.snapshot.metadata' requires the 'com.android.library' or " +
            "'com.android.application' plugin. Please apply one of them."
        )
      }
    }
  }
}
