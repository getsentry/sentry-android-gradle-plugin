package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.instrumentation.resolveClassAvailability
import io.sentry.android.gradle.util.artifactsFor
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * Resolves the variant runtime classpath at execution time and writes the optional SDK class
 * availability map used by
 * [io.sentry.android.gradle.instrumentation.SentrySdkOptimizationClassVisitorFactory].
 *
 * Keeping resolution on a task input avoids resolving `*RuntimeClasspath` during configuration when
 * AGP snapshots instrumentation parameters.
 */
@CacheableTask
abstract class ResolveSdkClassAvailabilityTask : DefaultTask() {

  init {
    description = "Resolves optional Sentry SDK class availability from the runtime classpath"
  }

  /** External module coordinates in `group:module` form. */
  @get:Input abstract val moduleIds: SetProperty<String>

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun action() {
    val modules =
      moduleIds
        .get()
        .mapNotNull { coordinate ->
          val parts = coordinate.split(':', limit = 2)
          if (parts.size != 2) {
            null
          } else {
            DefaultModuleIdentifier.newId(parts[0], parts[1])
          }
        }
        .toSet()

    val availability = resolveClassAvailability(modules)
    val output = outputFile.get().asFile
    output.parentFile?.mkdirs()
    // Write manually so the file stays deterministic for build caching (no Properties timestamp).
    output.bufferedWriter().use { writer ->
      availability.toSortedMap().forEach { (className, available) ->
        writer.append(className).append('=').append(available.toString()).append('\n')
      }
    }
  }

  companion object {
    fun register(
      project: Project,
      configurationName: String,
      attributeValueJar: String = "android-classes",
      taskSuffix: String,
    ): TaskProvider<ResolveSdkClassAvailabilityTask>? {
      val configuration =
        try {
          project.configurations.getByName(configurationName)
        } catch (e: UnknownDomainObjectException) {
          project.logger.warn(
            "Unable to find configuration $configurationName for SDK class availability."
          )
          return null
        }

      return project.tasks.register(
        "resolveSentrySdkClassAvailability$taskSuffix",
        ResolveSdkClassAvailabilityTask::class.java,
      ) { task ->
        // Lazy: artifactsFor(...).resolvedArtifacts only resolves when the task executes.
        val artifacts = configuration.artifactsFor(attributeValueJar).resolvedArtifacts
        task.moduleIds.set(
          artifacts.map { list ->
            list
              .map { artifact -> artifact.id.componentIdentifier }
              .filterIsInstance<ModuleComponentIdentifier>()
              .map { id -> "${id.group}:${id.module}" }
              .toSet()
          }
        )
        task.outputFile.set(
          project.layout.buildDirectory.file(
            "intermediates/sentry/classAvailability/${taskSuffix}.properties"
          )
        )
      }
    }
  }
}
