@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.telemetry

import io.sentry.BuildConfig
import io.sentry.IHub
import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.NoOpHub
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.android.gradle.SentryCliProvider
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.SentryPlugin.Companion.logger
import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryCliInfoValueSource.InfoParams
import io.sentry.android.gradle.telemetry.SentryCliVersionValueSource.VersionParams
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.SentryCliException
import io.sentry.android.gradle.util.error
import io.sentry.android.gradle.util.getBuildServiceName
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.setSentryPipelineEnv
import io.sentry.exception.ExceptionMechanismException
import io.sentry.gradle.common.SentryVariant
import io.sentry.protocol.Mechanism
import io.sentry.protocol.User
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.execution.ExecuteTaskBuildOperationDetails
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters.None
import org.gradle.api.tasks.Input
import org.gradle.execution.RunRootBuildWorkBuildOperationType
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent
import org.gradle.process.ExecOperations
import org.gradle.util.GradleVersion

abstract class SentryTelemetryService : BuildService<None>, BuildOperationListener, AutoCloseable {

  private var hub: IHub = NoOpHub.getInstance()
  private var transaction: ITransaction? = null
  private var didAddChildSpans: Boolean = false
  private var started: Boolean = false

  @Synchronized
  fun start(paramsCallback: () -> SentryTelemetryServiceParams) {
    if (started) {
      return
    }
    val startParameters = paramsCallback()

    try {
      if (startParameters.saas == false) {
        SentryPlugin.logger.info {
          "Sentry is running against a self hosted instance. " + "Telemetry has been disabled."
        }
        hub = NoOpHub.getInstance()
      } else if (!startParameters.sendTelemetry) {
        SentryPlugin.logger.info { "Sentry telemetry has been disabled." }
        hub = NoOpHub.getInstance()
      } else {
        if (!Sentry.isEnabled()) {
          SentryPlugin.logger.info {
            "Sentry telemetry is enabled. To disable set `telemetry=false` " +
              "in the sentry config block."
          }
          Sentry.init { options ->
            options.dsn = startParameters.dsn
            options.isDebug = startParameters.isDebug
            options.isEnablePrettySerializationOutput = false
            options.tracesSampleRate = 1.0
            options.release = BuildConfig.Version
            options.isSendModules = false
            options.environment = startParameters.buildType
            options.setTag("SDK_VERSION", BuildConfig.SdkVersion)
            options.setTag("BUILD_SYSTEM", "gradle")
            options.setTag("GRADLE_VERSION", GradleVersion.current().version)
            startParameters.cliVersion?.let { options.setTag("SENTRY_CLI_VERSION", it) }

            startParameters.extraTags.forEach { (key, value) -> options.setTag(key, value) }

            try {
              options.setTag("AGP_VERSION", AgpVersions.CURRENT.toString())
            } catch (t: Throwable) {}
          }
        }
        hub = Sentry.getCurrentHub()
        startRun("gradle build ${startParameters.buildType}")

        hub.configureScope { scope ->
          scope.user =
            User().also { user ->
              startParameters.defaultSentryOrganization?.let { org ->
                if (org != "-") {
                  user.id = org
                }
              }
              startParameters.sentryOrganization?.let { user.id = it }
            }
        }

        started = true
      }
    } catch (t: Throwable) {
      SentryPlugin.logger.error(t) { "Sentry failed to initialize." }
    }
  }

  override fun started(descriptor: BuildOperationDescriptor, event: OperationStartEvent) {}

  override fun progress(identifier: OperationIdentifier, event: OperationProgressEvent) {}

  override fun finished(
    buildOperationDescriptor: BuildOperationDescriptor,
    operationFinishEvent: OperationFinishEvent,
  ) {
    val details = buildOperationDescriptor.details

    operationFinishEvent.failure?.let { error ->
      if (isSentryError(error, details)) {
        captureError(error, "build")
        transaction?.status = SpanStatus.UNKNOWN_ERROR
      }
    }

    if (details is RunRootBuildWorkBuildOperationType.Details) {
      endRun()
    }
  }

  private fun isSentryError(throwable: Throwable, details: Any?): Boolean {
    val isSentryTaskName =
      (details as? ExecuteTaskBuildOperationDetails)?.let {
        it.task.name.substringAfterLast(":").contains("sentry", ignoreCase = true)
      } ?: false
    return isSentryTaskName ||
      throwable.stackTrace.any {
        it.className.startsWith("io.sentry") &&
          !(it.className.contains("test", ignoreCase = true) ||
            it.className.contains("rule", ignoreCase = true))
      }
  }

  fun captureError(exception: Throwable, operation: String?) {
    val message =
      if (exception is SentryCliException) {
        "$operation failed with SentryCliException and reason ${exception.reason}"
      } else {
        "$operation failed with ${exception.javaClass}"
      }

    val mechanism =
      Mechanism().also {
        it.type = MECHANISM_TYPE
        it.isHandled = false
      }
    val mechanismException: Throwable =
      ExceptionMechanismException(
        mechanism,
        SentryMinimalException(message),
        Thread.currentThread(),
      )
    val event = SentryEvent(mechanismException).also { it.level = SentryLevel.FATAL }
    hub.captureEvent(event)
  }

  fun startRun(transactionName: String) {
    hub.startSession()
    val options = TransactionOptions()
    options.isBindToScope = true
    transaction = hub.startTransaction(transactionName, "build", options)
  }

  fun endRun() {
    if (didAddChildSpans) {
      transaction?.finish()
      hub.endSession()
    }
  }

  fun traceCli(): List<String> {
    val args = mutableListOf<String>()
    hub.traceparent?.let { header ->
      args.add("--header")
      args.add("${header.name}:${header.value}")
    }
    hub.baggage?.let { header ->
      args.add("--header")
      args.add("${header.name}:${header.value}")
    }
    return args
  }

  fun startTask(operation: String): ISpan? {
    didAddChildSpans = true
    hub.setTag("step", operation)
    return hub.span?.startChild(operation)
  }

  fun endTask(span: ISpan?, task: Task) {
    span?.let { span ->
      task.state.failure?.let { throwable ->
        captureError(throwable, span.operation)
        span.status = SpanStatus.UNKNOWN_ERROR
      }

      span.finish()
    }
  }

  override fun close() {
    if (transaction?.isFinished == false) {
      endRun()
    }
    Sentry.close()
  }

  companion object {
    val SENTRY_SAAS_DSN: String =
      "https://000e5dea9770b4537055f8a6d28c021e@o1.ingest.sentry.io/4506241308295168"
    val MECHANISM_TYPE: String = "GradleTelemetry"
    private val orgRegex = Regex("""(?m)Default Organization: (.*)$""")
    private val versionRegex = Regex("""(?m)sentry-cli (.*)$""")

    fun createParameters(
      project: Project,
      variant: SentryVariant?,
      extension: SentryPluginExtension,
      cliExecutable: Provider<String>,
      sentryOrg: String?,
      buildType: String,
    ): SentryTelemetryServiceParams {
      val tags = extraTagsFromExtension(project, extension)
      val org = sentryOrg ?: extension.org.orNull
      val isTelemetryEnabled = extension.telemetry.get()

      // if telemetry is disabled we don't even need to exec sentry-cli as telemetry service
      // will be no-op
      if (isTelemetryEnabled) {
        paramsWithExecAvailable(project, cliExecutable, extension, variant, org, buildType, tags)
          ?.let {
            return it
          }
      }
      // fallback: sentry-cli is not available or e.g. auth token is not configured
      return SentryTelemetryServiceParams(
        isTelemetryEnabled,
        extension.telemetryDsn.get(),
        org,
        buildType,
        tags,
        extension.debug.get(),
        saas = extension.url.orNull == null,
        cliVersion = BuildConfig.CliVersion,
      )
    }

    private fun paramsWithExecAvailable(
      project: Project,
      cliExecutable: Provider<String>,
      extension: SentryPluginExtension,
      variant: SentryVariant?,
      sentryOrg: String?,
      buildType: String,
      tags: Map<String, String>,
    ): SentryTelemetryServiceParams? {
      var cliVersion: String? = BuildConfig.CliVersion
      var defaultSentryOrganization: String? = null
      val infoOutput =
        project.providers
          .of(SentryCliInfoValueSource::class.java) { cliVS ->
            cliVS.parameters.buildDirectory.set(project.buildDir)
            cliVS.parameters.cliExecutable.set(cliExecutable)
            cliVS.parameters.authToken.set(extension.authToken)
            cliVS.parameters.url.set(extension.url)
            variant?.let { v ->
              cliVS.parameters.propertiesFilePath.set(
                SentryPropertiesFileProvider.getPropertiesFilePath(project, v)
              )
            }
          }
          .get()

      if (infoOutput.isEmpty()) {
        return null
      }
      val isSaas = infoOutput.contains("(?m)Sentry Server: .*sentry.io$".toRegex())

      orgRegex.find(infoOutput)?.let { matchResult ->
        val groupValues = matchResult.groupValues
        if (groupValues.size > 1) {
          defaultSentryOrganization = groupValues[1]
        }
      }

      val versionOutput =
        project.providers
          .of(SentryCliVersionValueSource::class.java) { cliVS ->
            cliVS.parameters.buildDirectory.set(project.buildDir)
            cliVS.parameters.cliExecutable.set(cliExecutable)
            cliVS.parameters.url.set(extension.url)
          }
          .get()

      versionRegex.find(versionOutput)?.let { matchResult ->
        val groupValues = matchResult.groupValues
        if (groupValues.size > 1) {
          cliVersion = groupValues[1]
        }
      }

      return SentryTelemetryServiceParams(
        extension.telemetry.get(),
        extension.telemetryDsn.get(),
        sentryOrg,
        buildType,
        tags,
        extension.debug.get(),
        defaultSentryOrganization,
        isSaas,
        cliVersion = cliVersion,
      )
    }

    fun register(project: Project): Provider<SentryTelemetryService> {
      return project.gradle.sharedServices.registerIfAbsent(
        getBuildServiceName(SentryTelemetryService::class.java),
        SentryTelemetryService::class.java,
      ) {}
    }

    private fun extraTagsFromExtension(
      project: Project,
      extension: SentryPluginExtension,
    ): Map<String, String> {
      val tags = mutableMapOf<String, String>()

      tags.put("debug", extension.debug.get().toString())
      tags.put("includeProguardMapping", extension.includeProguardMapping.get().toString())
      tags.put("autoUploadProguardMapping", extension.autoUploadProguardMapping.get().toString())
      tags.put("autoUpload", extension.autoUpload.get().toString())
      tags.put("uploadNativeSymbols", extension.uploadNativeSymbols.get().toString())
      tags.put("autoUploadNativeSymbols", extension.autoUploadNativeSymbols.get().toString())
      tags.put("includeNativeSources", extension.includeNativeSources.get().toString())
      tags.put("ignoredVariants_set", extension.ignoredVariants.get().isNotEmpty().toString())
      tags.put("ignoredBuildTypes_set", extension.ignoredBuildTypes.get().isNotEmpty().toString())
      tags.put("ignoredFlavors_set", extension.ignoredFlavors.get().isNotEmpty().toString())
      tags.put("dexguardEnabled", extension.dexguardEnabled.get().toString())
      tags.put("tracing_enabled", extension.tracingInstrumentation.enabled.get().toString())
      tags.put("tracing_debug", extension.tracingInstrumentation.debug.get().toString())
      tags.put(
        "tracing_forceInstrumentDependencies",
        extension.tracingInstrumentation.forceInstrumentDependencies.get().toString(),
      )
      tags.put("tracing_features", extension.tracingInstrumentation.features.get().toString())
      tags.put(
        "tracing_logcat_enabled",
        extension.tracingInstrumentation.logcat.enabled.get().toString(),
      )
      tags.put(
        "tracing_logcat_minLevel",
        extension.tracingInstrumentation.logcat.minLevel.get().toString(),
      )
      tags.put("autoInstallation_enabled", extension.autoInstallation.enabled.get().toString())
      tags.put(
        "autoInstallation_sentryVersion",
        extension.autoInstallation.sentryVersion.get().toString(),
      )
      tags.put("includeDependenciesReport", extension.includeDependenciesReport.get().toString())
      tags.put("includeSourceContext", extension.includeSourceContext.get().toString())
      tags.put(
        "additionalSourceDirsForSourceContext_set",
        extension.additionalSourceDirsForSourceContext.get().isNotEmpty().toString(),
      )
      // TODO PII?
      //            extension.projectName.orNull?.let { tags.put("projectName", it) }

      return tags
    }
  }
}

class SentryMinimalException(message: String) : RuntimeException(message) {
  override fun getStackTrace(): Array<StackTraceElement> {
    val superStackTrace = super.getStackTrace()
    return if (superStackTrace.isEmpty()) superStackTrace else arrayOf(superStackTrace[0])
  }
}

abstract class SentryCliInfoValueSource : ValueSource<String, InfoParams> {
  interface InfoParams : ValueSourceParameters {
    @get:Input val buildDirectory: Property<File>

    @get:Input val cliExecutable: Property<String>

    @get:Input val propertiesFilePath: Property<String>

    @get:Input val url: Property<String>

    @get:Input val authToken: Property<String>
  }

  @get:Inject abstract val execOperations: ExecOperations

  override fun obtain(): String? {
    val stdOutput = ByteArrayOutputStream()
    val errOutput = ByteArrayOutputStream()

    val execResult =
      execOperations.exec {
        it.isIgnoreExitValue = true
        SentryCliProvider.maybeExtractFromResources(
          parameters.buildDirectory.get(),
          parameters.cliExecutable.get(),
        )

        val args = mutableListOf(parameters.cliExecutable.get())

        parameters.url.orNull?.let { url ->
          args.add("--url")
          args.add(url)
        }

        args.add("--log-level=error")
        args.add("info")

        parameters.propertiesFilePath.orNull?.let { path ->
          it.environment("SENTRY_PROPERTIES", path)
        }

        parameters.authToken.orNull?.let { authToken ->
          it.environment("SENTRY_AUTH_TOKEN", authToken)
        }

        it.setSentryPipelineEnv()

        it.commandLine(args)
        it.standardOutput = stdOutput
        it.errorOutput = errOutput
      }

    if (execResult.exitValue == 0) {
      return String(stdOutput.toByteArray(), Charset.defaultCharset())
    } else {
      logger.info {
        "Failed to execute sentry-cli info. Error Output: " +
          String(errOutput.toByteArray(), Charset.defaultCharset())
      }
      return ""
    }
  }
}

abstract class SentryCliVersionValueSource : ValueSource<String, VersionParams> {
  interface VersionParams : ValueSourceParameters {
    @get:Input val buildDirectory: Property<File>

    @get:Input val cliExecutable: Property<String>

    @get:Input val url: Property<String>
  }

  @get:Inject abstract val execOperations: ExecOperations

  override fun obtain(): String {
    val output = ByteArrayOutputStream()
    execOperations.exec {
      it.isIgnoreExitValue = true
      SentryCliProvider.maybeExtractFromResources(
        parameters.buildDirectory.get(),
        parameters.cliExecutable.get(),
      )

      val args = mutableListOf(parameters.cliExecutable.get())

      args.add("--log-level=error")
      args.add("--version")

      it.setSentryPipelineEnv()

      it.commandLine(args)
      it.standardOutput = output
    }
    return String(output.toByteArray(), Charset.defaultCharset())
  }
}

data class SentryTelemetryServiceParams(
  val sendTelemetry: Boolean,
  val dsn: String,
  val sentryOrganization: String?,
  val buildType: String,
  val extraTags: Map<String, String>,
  val isDebug: Boolean,
  val defaultSentryOrganization: String? = null,
  val saas: Boolean? = null,
  val cliVersion: String? = null,
)
