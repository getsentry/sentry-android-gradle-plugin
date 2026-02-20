package io.sentry.android.gradle.snapshot

import io.sentry.android.gradle.snapshot.preview.JsonSerializer
import io.sentry.android.gradle.snapshot.preview.PreviewScanner
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class ScanPreviewsTask : DefaultTask() {

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val classesDirs: ConfigurableFileCollection

  @get:Input abstract val includePrivatePreviews: Property<Boolean>

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun scan() {
    val configs =
      classesDirs.files
        .filter { it.exists() }
        .flatMap { PreviewScanner.scan(it, includePrivatePreviews.get()) }

    val json = JsonSerializer.serialize(configs)
    outputFile.get().asFile.apply {
      parentFile.mkdirs()
      writeText(json)
    }

    logger.lifecycle("Sentry: Found ${configs.size} @Preview composables")
  }

  companion object {
    fun register(
      project: Project,
      variantName: String,
      classesDirs: Any,
      includePrivatePreviews: Provider<Boolean>,
    ): TaskProvider<ScanPreviewsTask> {
      val taskName = "sentryScanPreviews${variantName.replaceFirstChar { it.titlecase() }}"
      return project.tasks.register(taskName, ScanPreviewsTask::class.java) { task ->
        task.classesDirs.from(classesDirs)
        task.includePrivatePreviews.set(includePrivatePreviews)
        task.outputFile.set(
          project.layout.buildDirectory.file(
            "intermediates/sentry/snapshot/$variantName/previewConfigs.json"
          )
        )
      }
    }
  }
}
