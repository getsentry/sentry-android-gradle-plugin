package io.sentry.android.gradle.snapshot

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import io.sentry.android.gradle.util.AgpVersions
import kotlin.jvm.java
import org.gradle.api.Plugin
import org.gradle.api.Project

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
        if (AgpVersions.isAGP90(AgpVersions.CURRENT)) {
          // Right now it seems we only have HostTestBuilder.UNIT_TEST_TYPE as the key but we are
          // creating screenshot tests like HostTestBuilder.SCREENSHOT_TEST_TYPE
          // We should adjust this once the API is stable and documented.
          variant.hostTests.values.forEach {
            // Using `sources?.kotlin` is broken so we have to use sources?.java:
            // https://issuetracker.google.com/issues/268248348
            it.sources.java?.addGeneratedSourceDirectory(
              generateTask,
              GenerateSnapshotTestsTask::outputDir,
            )
          }
        } else {
          // `unitTest` is deprecated, the replacement above is complex
          @Suppress("DEPRECATION_ERROR")
          variant.unitTest
            ?.sources
            ?.java
            ?.addGeneratedSourceDirectory(generateTask, GenerateSnapshotTestsTask::outputDir)
        }
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
