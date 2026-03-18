package io.sentry.android.gradle.snapshot

import com.android.build.gradle.BaseExtension
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

      val generateTask = GenerateSnapshotTestsTask.register(project, extension, android)

      project.dependencies.add(
        "testImplementation",
        "io.github.sergio-sastre.ComposablePreviewScanner:android:0.8.1",
      )

      // Wire source set and task dependencies eagerly — afterEvaluate is too late
      // for the Kotlin compiler to pick up the generated sources.
      android.sourceSets.getByName("test").kotlin.srcDir(generateTask.flatMap { it.outputDir })

      project.tasks.configureEach { task ->
        if (task.name.matches(Regex("(compile|ksp).*UnitTestKotlin"))) {
          task.dependsOn(generateTask)
        }
      }
    }

    project.afterEvaluate {
      if (!project.pluginManager.hasPlugin("app.cash.paparazzi")) {
        project.logger.warn(
          "WARNING: 'io.sentry.android.snapshot' requires the 'app.cash.paparazzi' plugin. " +
            "Please apply 'app.cash.paparazzi' to use snapshot testing."
        )
      }
    }
  }
}
