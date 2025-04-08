package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.debug
import io.sentry.android.gradle.util.getAndDelete
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * A synthetic task to
 */
abstract class ExplodeSourcesTasks : DefaultTask() {

  init {
    group = SENTRY_GROUP
    description = "Flattens sources and dumps their paths into a single file."

    @Suppress("LeakingThis") onlyIf { includeSourceContext.getOrElse(false) }
  }

  @get:Input
  abstract val includeSourceContext: Property<Boolean>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val sourceDirs: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    val outPath = output.getAndDelete()
    // flatten the sourceDirs tree into a single list of relative filenames and write to a file
    val sourceFiles = mutableListOf<String>()
    sourceDirs.files.forEach { dir ->
      dir.walkTopDown().forEach { file ->
        if (file.isFile) {
          val sourceFileInBundle = file.relativeTo(dir).path
          val sourceFileInRoot = file.relativeTo(project.rootDir).path
          if (sourceFileInRoot.isBlank() || sourceFileInBundle.isBlank()) {
            project.logger.debug {
              "Skipping ${file.absolutePath} as the plugin was unable to determine a relative path for it."
            }
          } else {
            sourceFiles.add("$sourceFileInRoot:$sourceFileInBundle")
          }
        }
      }
    }
    outPath.writeText(sourceFiles.joinToString("\n"))
  }

  companion object {
    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>?,
      includeSourceContext: Property<Boolean>,
      sourceDirs: Provider<out Collection<Directory>>?,
      output: Provider<RegularFile>,
      taskSuffix: String = "",
    ): TaskProvider<ExplodeSourcesTasks> {
      return project.tasks.register(
        "sentryRepublishSources$taskSuffix",
        ExplodeSourcesTasks::class.java,
      ) { task ->
        task.sourceDirs.setFrom(sourceDirs)
        task.includeSourceContext.set(includeSourceContext)
        task.output.set(output)
        task.withSentryTelemetry(extension, sentryTelemetryProvider)
      }
    }
  }
}

internal fun SentryVariant.configureRepublishSourcesTask(
  project: Project,
  extension: SentryPluginExtension,
  sentryTelemetryProvider: Provider<SentryTelemetryService>?,
  includeSourceContext: Property<Boolean>,
  sourceRoots: Provider<out Collection<Directory>>?,
  output: Provider<RegularFile>,
): TaskProvider<ExplodeSourcesTasks> {
  val explodeSourcesTasks =
    ExplodeSourcesTasks.register(
      project = project,
      extension = extension,
      sentryTelemetryProvider = sentryTelemetryProvider,
      includeSourceContext = includeSourceContext,
      sourceDirs = sourceRoots,
      output = output,
      taskSuffix = name.capitalized,
    )
  return explodeSourcesTasks
}