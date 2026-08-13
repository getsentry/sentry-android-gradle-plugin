package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.ManifestMetadataParser
import io.sentry.android.gradle.instrumentation.resolveClassAvailability
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class GenerateSentryBuildTimeOptionsTask : DirectoryOutputTask() {

  @get:Input abstract val moduleIds: SetProperty<String>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val mergedManifest: RegularFileProperty

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
    val metadata = ManifestMetadataParser.parse(mergedManifest.get().asFile)
    val sourceFile = output.file(GENERATED_CLASS_PATH).get().asFile
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

        public static Map<String, Object> getManifestMetadata() {
      ${
        if (metadata == null) {
          "    return null;"
        } else {
          """    Map<String, Object> metadata = new HashMap<>();
      ${
            metadata.toSortedMap().entries.joinToString("\n") { (key, value) ->
              "    metadata.put(${key.javaStringLiteral()}, ${value.javaLiteral()});"
            }
          }
          return metadata;"""
        }
      }
        }
      }
      """
        .trimIndent() + "\n"
    )
  }

  companion object {
    private const val GENERATED_CLASS_PATH =
      "io/sentry/android/core/SentryGeneratedBuildTimeOptions.java"

    private fun String.javaStringLiteral(): String = buildString {
      append('"')
      this@javaStringLiteral.forEach { character ->
        append(
          when (character) {
            '\\' -> "\\\\"
            '"' -> "\\\""
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            else -> character
          }
        )
      }
      append('"')
    }

    private fun Any.javaLiteral(): String =
      when (this) {
        is Boolean,
        is Int -> toString()
        is Float -> "${this}f"
        is String -> javaStringLiteral()
        else -> error("Unsupported manifest metadata value: $this")
      }

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
