package io.sentry.android.gradle.tasks

import groovy.json.JsonOutput
import io.sentry.android.gradle.ManifestMetadataParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ParseSentryManifestMetadataTask : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val mergedManifest: RegularFileProperty

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun parse() {
    val metadata = ManifestMetadataParser.parse(mergedManifest.get().asFile)
    val json = JsonOutput.toJson(metadata?.toSortedMap())
    outputFile.get().asFile.apply {
      parentFile.mkdirs()
      writeText(json, Charsets.UTF_8)
    }
  }
}
