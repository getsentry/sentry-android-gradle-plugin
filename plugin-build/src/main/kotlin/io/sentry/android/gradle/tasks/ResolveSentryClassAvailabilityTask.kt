package io.sentry.android.gradle.tasks

import groovy.json.JsonOutput
import io.sentry.android.gradle.instrumentation.resolveClassAvailability
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ResolveSentryClassAvailabilityTask : DefaultTask() {

  @get:Input abstract val moduleIds: SetProperty<String>

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun resolve() {
    val modules =
      moduleIds
        .get()
        .mapNotNull { coordinate ->
          val parts = coordinate.split(':', limit = 2)
          if (parts.size == 2) DefaultModuleIdentifier.newId(parts[0], parts[1]) else null
        }
        .toSet()
    val json = JsonOutput.toJson(resolveClassAvailability(modules).toSortedMap())
    outputFile.get().asFile.apply {
      parentFile.mkdirs()
      writeText(json, Charsets.UTF_8)
    }
  }
}
