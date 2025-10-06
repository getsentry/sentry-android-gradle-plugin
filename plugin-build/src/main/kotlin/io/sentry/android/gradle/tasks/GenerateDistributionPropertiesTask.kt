package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class GenerateDistributionPropertiesTask : PropertiesFileOutputTask() {

  init {
    description = "Writes properties used to check for Build Distribution updates"
  }

  @get:OutputFile
  override val outputFile: Provider<RegularFile>
    get() = output.file(SENTRY_DISTRIBUTION_OUTPUT)

  @get:Input abstract val orgSlug: Property<String>

  @get:Input abstract val projectSlug: Property<String>

  @get:Input abstract val orgAuthToken: Property<String>

  @get:Input abstract val buildConfiguration: Property<String>

  @TaskAction
  fun generateProperties() {
    outputFile.get().asFile.writer().use { writer ->
      orgSlug.orNull?.let { writer.appendLine("$ORG_SLUG_PROPERTY=$it") }
      projectSlug.orNull?.let { writer.appendLine("$PROJECT_SLUG_PROPERTY=$it") }
      orgAuthToken.orNull?.let { writer.appendLine("$ORG_AUTH_TOKEN_PROPERTY=$it") }
      writer.appendLine("$BUILD_CONFIGURATION_PROPERTY=${buildConfiguration.get()}")
    }
  }

  companion object {
    internal const val SENTRY_DISTRIBUTION_OUTPUT = "sentry-distribution.properties"
    const val ORG_SLUG_PROPERTY = "io.sentry.distribution.org-slug"
    const val PROJECT_SLUG_PROPERTY = "io.sentry.distribution.project-slug"
    const val ORG_AUTH_TOKEN_PROPERTY = "io.sentry.distribution.org-auth-token"
    const val BUILD_CONFIGURATION_PROPERTY = "io.sentry.distribution.build-configuration"

    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>? = null,
      output: Provider<Directory>,
      taskSuffix: String,
      buildConfiguration: String,
    ): TaskProvider<GenerateDistributionPropertiesTask> {
      return project.tasks.register(
        "generateSentryDistributionProperties$taskSuffix",
        GenerateDistributionPropertiesTask::class.java,
      ) { task ->
        task.output.set(output)
        task.withSentryTelemetry(extension, sentryTelemetryProvider)
        task.orgSlug.set(extension.org)
        task.projectSlug.set(extension.projectName)
        task.orgAuthToken.set(extension.authToken)
        task.buildConfiguration.set(buildConfiguration)
      }
    }
  }
}
