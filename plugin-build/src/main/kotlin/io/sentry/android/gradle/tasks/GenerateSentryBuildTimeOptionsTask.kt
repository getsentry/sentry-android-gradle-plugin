package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.instrumentation.resolveClassAvailability
import java.io.File
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class GenerateSentryBuildTimeOptionsTask : DirectoryOutputTask() {

  @get:Input abstract val moduleIds: SetProperty<String>

  @TaskAction
  fun generate() {
    val modules =
      moduleIds
        .get()
        .mapNotNull { coordinate ->
          val parts = coordinate.split(':', limit = 2)
          if (parts.size == 2) DefaultModuleIdentifier.newId(parts[0], parts[1]) else null
        }
        .toSet()
    val availability = resolveClassAvailability(modules)
    val sourceFile = File(output.get().asFile, GENERATED_CLASS_PATH)
    sourceFile.parentFile.mkdirs()
    sourceFile.writeText(
      """
      package io.sentry.android.core;

      import java.util.HashMap;
      import java.util.Map;

      public final class SentryGeneratedBuildTimeOptions {
        private SentryGeneratedBuildTimeOptions() {}

        public static Map<String, Boolean> getClassAvailability() {
          Map<String, Boolean> availability = new HashMap<>();
      ${
        availability.toSortedMap().entries.joinToString("\n") { (className, available) ->
          "    availability.put(\"$className\", $available);"
        }
      }
          return availability;
        }
      }
      """
        .trimIndent() + "\n"
    )
  }

  companion object {
    private const val GENERATED_CLASS_PATH =
      "io/sentry/android/core/SentryGeneratedBuildTimeOptions.java"

    fun register(
      project: Project,
      configurationName: String,
      taskSuffix: String,
    ): TaskProvider<GenerateSentryBuildTimeOptionsTask>? {
      val configurationProvider =
        try {
          project.configurations.named(configurationName)
        } catch (e: UnknownDomainObjectException) {
          project.logger.warn(
            "Unable to find configuration $configurationName for Sentry build-time options."
          )
          return null
        }

      return project.tasks.register(
        "generateSentryBuildTimeOptions$taskSuffix",
        GenerateSentryBuildTimeOptionsTask::class.java,
      ) { task ->
        task.moduleIds.set(
          configurationProvider.map { configuration ->
            configuration.incoming.resolutionResult.allComponents.mapNotNullTo(mutableSetOf()) {
              component ->
              component.moduleVersion?.module?.let { "${it.group}:${it.name}" }
            }
          }
        )
        task.output.set(
          project.layout.buildDirectory.dir("generated/sentry/buildTimeOptions/$taskSuffix")
        )
      }
    }
  }
}
