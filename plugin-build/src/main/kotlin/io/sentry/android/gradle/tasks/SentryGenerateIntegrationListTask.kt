package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.ManifestWriter
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.services.SentryModulesService
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.info
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class SentryGenerateIntegrationListTask : DefaultTask() {

  init {
    description = "Writes enabled integrations to AndroidManifest.xml"
  }

  // we only care about contents
  @get:PathSensitive(NONE) @get:InputFile abstract val mergedManifest: RegularFileProperty

  @get:OutputFile abstract val updatedManifest: RegularFileProperty

  @get:Input abstract val integrations: SetProperty<String>

  @TaskAction
  fun writeIntegrationListToManifest() {
    logger.info { "SentryGenerateIntegrationListTask - outputFile: ${updatedManifest.get()}" }
    val integrations = integrations.get()
    val manifestFile = mergedManifest.asFile.get()
    val updatedManifestFile = updatedManifest.asFile.get()

    if (integrations.isNotEmpty()) {
      val manifestWriter = ManifestWriter()
      val integrationsList = integrations.toList().sorted().joinToString(",")
      manifestWriter.writeMetaData(
        manifestFile,
        updatedManifestFile,
        ATTR_INTEGRATIONS,
        integrationsList,
      )
    } else {
      logger.info { "No Integrations present, copying input manifest to output" }
      Files.copy(
        manifestFile.toPath(),
        updatedManifestFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
      )
    }
  }

  companion object {
    const val ATTR_INTEGRATIONS = "io.sentry.gradle-plugin-integrations"

    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>?,
      sentryModulesService: Provider<SentryModulesService>,
      variantName: String,
    ): TaskProvider<SentryGenerateIntegrationListTask> {
      return project.tasks.register(
        "${variantName}SentryGenerateIntegrationListTask",
        SentryGenerateIntegrationListTask::class.java,
      ) {
        it.integrations.set(
          sentryModulesService.flatMap { service ->
            service.retrieveEnabledInstrumentationFeatures(project)
          }
        )
        it.usesService(sentryModulesService)
        it.withSentryTelemetry(extension, sentryTelemetryProvider)
      }
    }
  }
}
