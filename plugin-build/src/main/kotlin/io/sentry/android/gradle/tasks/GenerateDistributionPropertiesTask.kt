package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.PropertiesUtil
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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

  @get:Optional
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sentryPropertiesFile: RegularFileProperty

  @get:Input abstract val orgSlug: Property<String>

  @get:Input abstract val projectSlug: Property<String>

  @get:Input abstract val distributionAuthToken: Property<String>

  @get:Input abstract val buildConfiguration: Property<String>

  @get:Input @get:Optional abstract val installGroups: SetProperty<String>

  @TaskAction
  fun generateProperties() {
    outputFile.get().asFile.writer().use { writer ->
      orgSlug.orNull?.let { writer.appendLine("$ORG_SLUG_PROPERTY=$it") }
      projectSlug.orNull?.let { writer.appendLine("$PROJECT_SLUG_PROPERTY=$it") }
      distributionAuthToken.orNull?.let {
        writer.appendLine("$DISTRIBUTION_AUTH_TOKEN_PROPERTY=$it")
      }
      writer.appendLine("$BUILD_CONFIGURATION_PROPERTY=${buildConfiguration.get()}")
      installGroups.orNull
        ?.takeIf { it.isNotEmpty() }
        ?.let { writer.appendLine("$INSTALL_GROUPS_PROPERTY=${it.joinToString(",")}") }
    }
  }

  companion object {
    internal const val SENTRY_DISTRIBUTION_OUTPUT = "sentry-distribution.properties"
    const val ORG_SLUG_PROPERTY = "io.sentry.distribution.org-slug"
    const val PROJECT_SLUG_PROPERTY = "io.sentry.distribution.project-slug"
    const val DISTRIBUTION_AUTH_TOKEN_PROPERTY = "io.sentry.distribution.auth-token"
    const val BUILD_CONFIGURATION_PROPERTY = "io.sentry.distribution.build-configuration"
    const val INSTALL_GROUPS_PROPERTY = "io.sentry.distribution.install-groups-override"

    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>? = null,
      output: Provider<Directory>,
      taskSuffix: String,
      buildConfiguration: String,
      sentryOrg: String? = null,
      sentryProject: String? = null,
      variant: SentryVariant? = null,
    ): TaskProvider<GenerateDistributionPropertiesTask> {
      return project.tasks.register(
        "generateSentryDistributionProperties$taskSuffix",
        GenerateDistributionPropertiesTask::class.java,
      ) { task ->
        task.output.set(output)
        task.withSentryTelemetry(extension, sentryTelemetryProvider)

        // Set sentry.properties file as task input if available
        val sentryPropertiesFilePath =
          variant?.let { SentryPropertiesFileProvider.getPropertiesFilePath(project, it) }
        if (sentryPropertiesFilePath != null) {
          task.sentryPropertiesFile.set(File(sentryPropertiesFilePath))
        }

        // Resolve org slug with fallback chain: ext -> extension -> env -> sentry.properties
        val orgProvider =
          project.provider {
            sentryOrg
              ?: extension.org.orNull
              ?: System.getenv("SENTRY_ORG")
              ?: task.sentryPropertiesFile.orNull?.asFile?.let { file ->
                PropertiesUtil.loadMaybe(file)?.let { props ->
                  props.getProperty("defaults.org") ?: props.getProperty("org")
                }
              }
          }
        task.orgSlug.set(orgProvider)

        // Resolve project slug with fallback chain: ext -> extension -> env -> sentry.properties
        val projectProvider =
          project.provider {
            sentryProject
              ?: extension.projectName.orNull
              ?: System.getenv("SENTRY_PROJECT")
              ?: task.sentryPropertiesFile.orNull?.asFile?.let { file ->
                PropertiesUtil.loadMaybe(file)?.let { props ->
                  props.getProperty("defaults.project") ?: props.getProperty("project")
                }
              }
          }
        task.projectSlug.set(projectProvider)

        task.distributionAuthToken.set(extension.distribution.authToken)

        task.buildConfiguration.set(buildConfiguration)

        task.installGroups.set(extension.distribution.installGroups)
      }
    }
  }
}
