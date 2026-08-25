package io.sentry.android.gradle.tasks.optimization

import groovy.json.JsonSlurper
import io.sentry.android.gradle.ManifestMetadataParser
import io.sentry.android.gradle.tasks.DirectoryOutputTask
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class GenerateSentryBuildTimeOptionsTask : DirectoryOutputTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val classAvailabilityFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val manifestMetadataFile: RegularFileProperty

  @TaskAction
  fun generate() {
    val json = JsonSlurper()
    val availability =
      (json.parseText(classAvailabilityFile.get().asFile.readText(Charsets.UTF_8)) as Map<*, *>)
        .map { (key, value) -> key as String to value as Boolean }
        .toMap()
    val parsedMetadata = json.parseText(manifestMetadataFile.get().asFile.readText(Charsets.UTF_8))
    val metadata =
      if (parsedMetadata == null) {
        null
      } else {
        runCatching {
            (parsedMetadata as Map<*, *>)
              .map { (key, value) ->
                key as String to ManifestMetadataParser.inferType(value as String)
              }
              .toMap()
          }
          .onFailure {
            logger.info(
              "Sentry manifest metadata types could not be inferred for optimization.",
              it,
            )
          }
          .getOrNull()
      }
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
        .trimIndent() + "\n",
      Charsets.UTF_8,
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
      mergedManifest: Provider<RegularFile>,
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

      val intermediateDir =
        project.layout.buildDirectory.dir("intermediates/sentry/buildTimeOptions/$taskSuffix")
      val availabilityTask =
        project.tasks.register(
          "resolveSentryClassAvailability$taskSuffix",
          ResolveSentryClassAvailabilityTask::class.java,
        ) { task ->
          task.moduleIds.set(
            configurationProvider.map { configuration ->
              configuration.incoming.resolutionResult.allComponents.mapNotNullTo(mutableSetOf()) {
                component ->
                component.moduleVersion?.module?.let { "${it.group}:${it.name}" }
              }
            }
          )
          task.outputFile.set(intermediateDir.map { it.file("class-availability.json") })
        }
      val metadataTask =
        project.tasks.register(
          "parseSentryManifestMetadata$taskSuffix",
          ParseSentryManifestMetadataTask::class.java,
        ) { task ->
          task.mergedManifest.set(mergedManifest)
          task.outputFile.set(intermediateDir.map { it.file("manifest-metadata.json") })
        }

      return project.tasks.register(
        "generateSentryBuildTimeOptions$taskSuffix",
        GenerateSentryBuildTimeOptionsTask::class.java,
      ) { task ->
        task.classAvailabilityFile.set(availabilityTask.flatMap { it.outputFile })
        task.manifestMetadataFile.set(metadataTask.flatMap { it.outputFile })
        task.output.set(
          project.layout.buildDirectory.dir("generated/sentry/buildTimeOptions/$taskSuffix")
        )
      }
    }
  }
}
