package io.sentry.android.gradle.snapshot

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasUnitTest
import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.java

class SentrySnapshotPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension =
      project.extensions.create(
        "sentrySnapshot",
        SentrySnapshotExtension::class.java,
        project.objects,
      )

    project.pluginManager.withPlugin("app.cash.paparazzi") {
      val android = project.extensions.getByType(BaseExtension::class.java)

      project.dependencies.add(
        "testImplementation",
        "io.github.sergio-sastre.ComposablePreviewScanner:android:0.8.1",
      )

      val androidComponents =
        project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

      androidComponents.onVariants { variant ->
        val generateTask = GenerateSnapshotTestsTask.register(project, extension, android, variant)
        // `unitTest` is deprecated and it is unclear what the replacement is
        // Using `source?.kotlin` is broken so we have to use java: https://issuetracker.google.com/issues/268248348
        variant.unitTest?.sources?.java?.addGeneratedSourceDirectory(
          generateTask,
          GenerateSnapshotTestsTask::outputDir
        )
      }
    }

    project.afterEvaluate {
      if (!project.pluginManager.hasPlugin("app.cash.paparazzi")) {
        error(
          "'io.sentry.android.snapshot' requires the 'app.cash.paparazzi' plugin. " +
            "Please apply 'app.cash.paparazzi' to use snapshot testing."
        )
      }
    }
  }
}
