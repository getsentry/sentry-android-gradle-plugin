package io.sentry.android.gradle.tasks

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryCliProvider
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.util.info
import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "abstract task, should not be used directly")
abstract class SentryCliExecTask : Exec() {

  @get:Input @get:Optional abstract val debug: Property<Boolean>

  @get:Input abstract val cliExecutable: Property<String>

  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sentryProperties: RegularFileProperty

  @get:Input @get:Optional abstract val sentryOrganization: Property<String>

  @get:Input @get:Optional abstract val sentryProject: Property<String>

  @get:Input @get:Optional abstract val sentryAuthToken: Property<String>

  @get:Input @get:Optional abstract val sentryUrl: Property<String>

  @get:Internal abstract val sentryTelemetryService: Property<SentryTelemetryService>

  private val buildDirectory: Provider<File> = project.layout.buildDirectory.asFile

  override fun exec() {
    computeCommandLineArgs().let {
      commandLine(it)
      logger.info { "cli args: $it" }
    }
    setSentryPropertiesEnv()
    setSentryAuthTokenEnv()
    setSentryPipelineEnv()
    super.exec()
  }

  abstract fun getArguments(args: MutableList<String>)

  private fun preArgs(): List<String> {
    val args = mutableListOf<String>()

    sentryUrl.orNull?.let {
      args.add("--url")
      args.add(it)
    }

    if (debug.getOrElse(false)) {
      args.add("--log-level=debug")
    }

    sentryTelemetryService.orNull?.traceCli()?.let { args.addAll(it) }

    return args
  }

  private fun postArgs(args: MutableList<String>): List<String> {
    sentryOrganization.orNull?.let {
      args.add("--org")
      args.add(it)
    }

    sentryProject.orNull?.let {
      args.add("--project")
      args.add(it)
    }
    return args
  }

  /** Computes the full list of arguments for the task */
  fun computeCommandLineArgs(): List<String> {
    val args = mutableListOf<String>()
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
      args.add(0, "cmd")
      args.add(1, "/c")
    }

    val cliPath =
      SentryCliProvider.maybeExtractFromResources(buildDirectory.get(), cliExecutable.get())
    args.add(cliPath)
    args.addAll(preArgs())

    getArguments(args)

    return postArgs(args)
  }

  internal fun setSentryPropertiesEnv() {
    val sentryProperties = sentryProperties.orNull
    if (sentryProperties != null) {
      environment("SENTRY_PROPERTIES", sentryProperties)
    } else {
      logger.info { "propsFile is null" }
    }
  }

  internal fun setSentryAuthTokenEnv() {
    val sentryAuthToken = sentryAuthToken.orNull
    if (sentryAuthToken != null) {
      environment("SENTRY_AUTH_TOKEN", sentryAuthToken)
    } else {
      logger.info { "sentryAuthToken is null" }
    }
  }

  internal fun setSentryPipelineEnv() {
    environment("SENTRY_PIPELINE", "sentry-gradle-plugin/${BuildConfig.Version}")
  }
}
