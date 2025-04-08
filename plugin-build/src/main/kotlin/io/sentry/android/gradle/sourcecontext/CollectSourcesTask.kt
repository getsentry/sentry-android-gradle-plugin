package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.debug
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class CollectSourcesTask : DirectoryOutputTask() {

  init {
    group = SENTRY_GROUP
    description = "Collects sources into a single directory so they can be bundled together."

    @Suppress("LeakingThis") onlyIf { includeSourceContext.getOrElse(false) }
  }

  @get:Input abstract val includeSourceContext: Property<Boolean>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val sourceDirs: ConfigurableFileCollection

  @TaskAction
  fun action() {
    val outDir = output.getAndDelete()
    outDir.mkdirs()
    sourceDirs.singleFile.forEachLine { line ->
      val (sourceFileInRoot, sourceFileInBundle) = line.split(":")
      val sourceFile = File(sourceFileInRoot)
      if (sourceFile.isFile) {
        val targetFile = outDir.resolve(sourceFileInBundle)
        SentryPlugin.logger.debug {
          "Copying file ${sourceFile.absolutePath} " + "to ${targetFile.absolutePath}"
        }
        sourceFile.copyTo(targetFile, true)
      }
    }
  }

  companion object {
    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>?,
      sourceDirs: Provider<FileCollection>?,
      output: Provider<Directory>,
      includeSourceContext: Property<Boolean>,
      taskSuffix: String = "",
    ): TaskProvider<CollectSourcesTask> {
      return project.tasks.register(
        "sentryCollectSources$taskSuffix",
        CollectSourcesTask::class.java,
      ) { task ->
        task.sourceDirs.setFrom(sourceDirs)
        task.output.set(output)
        task.includeSourceContext.set(includeSourceContext)
        task.withSentryTelemetry(extension, sentryTelemetryProvider)
      }
    }
  }
}

internal class SourceCollector {

  fun collectSources(outDir: File, sourceDirs: ConfigurableFileCollection) {
    sourceDirs.forEach { sourceDir ->
      if (sourceDir.exists()) {
        SentryPlugin.logger.debug { "Collecting sources in ${sourceDir.absolutePath}" }
        sourceDir.walk().forEach { sourceFile ->
          val relativePath =
            sourceFile.absolutePath
              .removePrefix(sourceDir.absolutePath)
              .removePrefix(File.separator)
          val targetFile = outDir.resolve(File(relativePath))
          if (sourceFile.isFile) {
            if (relativePath.isBlank()) {
              SentryPlugin.logger.debug {
                "Skipping ${sourceFile.absolutePath} as the plugin was unable to determine a relative path for it."
              }
            } else {
              SentryPlugin.logger.debug {
                "Copying file ${sourceFile.absolutePath} " + "to ${targetFile.absolutePath}"
              }
              sourceFile.copyTo(targetFile, true)
            }
          }
        }
      } else {
        SentryPlugin.logger.debug {
          "Skipping source collection in ${sourceDir.absolutePath} as it doesn't " + "exist."
        }
      }
    }
  }
}

internal fun DirectoryProperty.getAndDelete(): File {
  val file = get().asFile
  if (file.isDirectory) {
    file.deleteRecursively()
  } else {
    file.delete()
  }
  return file
}
