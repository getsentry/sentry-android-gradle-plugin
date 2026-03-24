package io.sentry.android.gradle.snapshot

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasUnitTest
import com.android.build.api.variant.UnitTest
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

      val generateTask = GenerateSnapshotTestsTask.register(project, extension, android)

      project.dependencies.add(
        "testImplementation",
        "io.github.sergio-sastre.ComposablePreviewScanner:android:0.8.1",
      )

      val androidComponents =
        project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

      androidComponents.onVariants { variant ->
        (variant as? HasUnitTest)?.unitTest?.sources?.kotlin?.addGeneratedSourceDirectory(
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
