package io.sentry.android.gradle.telemetry

import io.sentry.BaggageHeader
import io.sentry.BuildConfig
import io.sentry.IHub
import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.NoOpHub
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryTraceHeader
import io.sentry.SpanStatus
import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SentryCliException
import io.sentry.android.gradle.util.getBuildServiceName
import io.sentry.exception.ExceptionMechanismException
import io.sentry.gradle.common.SentryVariant
import io.sentry.protocol.Mechanism
import io.sentry.protocol.User
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.execution.RunRootBuildWorkBuildOperationType
import org.gradle.execution.taskgraph.NotifyTaskGraphWhenReadyBuildOperationType
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent
import org.gradle.process.ExecOutput
import org.gradle.util.GradleVersion

abstract class SentryTelemetryService :
    BuildService<SentryTelemetryService.Params>,
    BuildOperationListener,
    AutoCloseable {
    interface Params : BuildServiceParameters {
        @get:Input
        val sendTelemetry: Property<Boolean>

        @get:Input
        val dsn: Property<String>

        @get:Input
        @get:Optional
        val sentryOrganization: Property<String>

        @get:Input
        @get:Optional
        val defaultSentryOrganization: Property<String>

        @get:Input
        val buildType: Property<String>

        @get:Input
        val extraTags: MapProperty<String, String>

        @get:Input
        @get:Optional
        val saas: Property<Boolean>

        @get:Input
        @get:Optional
        val cliVersion: Property<String>
    }

    private val hub: IHub
    private var transaction: ITransaction? = null
    private var configPhaseSpan: ISpan? = null
    private var executionPhaseSpan: ISpan? = null
    private var didAddChildSpans: Boolean = false

    init {
        // TODO duplicate transaction SentryPlugin and SentryJvmPlugin
        // TODO config phase errors are not reported because this Service is never initialized
        if (parameters.saas.orNull == false) {
            println(
                "Sentry is running against a self hosted instance. Telemetry has been disabled."
            )
            hub = NoOpHub.getInstance()
        } else if (!parameters.sendTelemetry.get()) {
            println("Sentry telemetry has been disabled.")
            hub = NoOpHub.getInstance()
        } else {
            if (!Sentry.isEnabled()) {
                // TODO test telemetry disabled
                println(
                    "Sentry telemetry is enabled. To disable set `telemetry=false` " +
                        "in the sentry config block."
                )
                // TODO Logs a message, telemetry is enabled and how to disable it
                Sentry.init { options ->
                    options.dsn = parameters.dsn.get()
                    options.isDebug = true
                    options.isEnablePrettySerializationOutput = false
                    options.tracesSampleRate = 1.0
                    options.release = BuildConfig.Version
                    options.isSendModules = false
                    options.environment = parameters.buildType.get()
                    options.setTag("SDK_VERSION", BuildConfig.SdkVersion)
                    options.setTag("AGP_VERSION", AgpVersions.CURRENT.toString())
                    options.setTag("BUILD_SYSTEM", "gradle")
                    options.setTag("BUILD_TYPE", parameters.buildType.get())
                    options.setTag("GRADLE_VERSION", GradleVersion.current().version)
                    parameters.cliVersion.orNull?.let { options.setTag("SENTRY_CLI_VERSION", it) }

                    parameters.extraTags.orNull?.forEach { (key, value) ->
                        options.setTag(
                            key,
                            value
                        )
                    }
                }
            }
            hub = Sentry.getCurrentHub()
            startRun("gradle build ${parameters.buildType.get()}")

            configPhaseSpan = hub.span?.startChild("configuration phase")
            hub.configureScope { scope ->
                scope.user = User().also { user ->
                    parameters.defaultSentryOrganization.orNull?.let { user.id = it }
                    parameters.sentryOrganization.orNull?.let { user.id = it }
                }
            }
        }
    }

    override fun started(descriptor: BuildOperationDescriptor, event: OperationStartEvent) {}

    override fun progress(identifier: OperationIdentifier, event: OperationProgressEvent) {}

    override fun finished(
        buildOperationDescriptor: BuildOperationDescriptor,
        operationFinishEvent: OperationFinishEvent
    ) {
        val details = buildOperationDescriptor.details
        if (details is NotifyTaskGraphWhenReadyBuildOperationType.Details) {
            configPhaseSpan?.finish()
            executionPhaseSpan = hub.span?.startChild("execution phase")
        }

        operationFinishEvent.failure?.let { error ->
            captureError(error, "build")
            transaction?.status = SpanStatus.UNKNOWN_ERROR
        }

        if (details is RunRootBuildWorkBuildOperationType.Details) {
            executionPhaseSpan?.finish()
            endRun()
        }
    }

    fun captureError(exception: Throwable, operation: String?) {
        // TODO how specific can we get without risking PII?
        val message = if (exception is SentryCliException) {
            "$operation failed with SentryCliException and reason ${exception.reason}"
        } else {
            "$operation failed with ${exception.javaClass}"
        }

        val mechanism = Mechanism().also {
            it.type = MECHANISM_TYPE
            it.isHandled = false
        }
        val mechanismException: Throwable =
            ExceptionMechanismException(
                mechanism,
                SentryMinimalException(message),
                Thread.currentThread()
            )
        val event = SentryEvent(mechanismException).also {
            it.level = SentryLevel.FATAL
        }
        hub.captureEvent(event)
    }

    fun startRun(operaton: String) {
        hub.startSession()
        transaction = hub.startTransaction("gradle run", "op1", true)
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

    fun getTraceHeader(): SentryTraceHeader? {
        return hub.traceparent
    }

    fun getBaggageHeader(): BaggageHeader? {
        return hub.baggage
    }

    fun captureTask(operation: String, callback: () -> Unit) {
        // do nothing for now
    }
    fun captureTask2(operation: String, callback: () -> Unit) {
        val span = hub.span?.startChild(operation)
        didAddChildSpans = true
        hub.setTag("step", operation)
        try {
            callback()
            span?.status = SpanStatus.OK
        } catch (t: Throwable) {
            captureError(t, operation)
            span?.status = SpanStatus.UNKNOWN_ERROR
        } finally {
            span?.finish()
        }
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

            span.setData("gradle_skipped", task.state.skipped)
            span.setData("gradle_uptodate", task.state.upToDate)
            span.setData("gradle_executed", task.state.executed)
            span.setData("gradle_didwork", task.state.didWork)
            span.setData("gradle_nosource", task.state.noSource)

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
            "https://dd1f82ad30a331bd7def2a0dce926c6e@o447951.ingest.sentry.io/4506031723446272"
        val MECHANISM_TYPE: String = "GradleTelemetry"
        private val orgRegex = Regex("""(?m)Default Organization: (.*)$""")
        private val versionRegex = Regex("""(?m)sentry-cli (.*)$""")

        fun register(
            project: Project,
            variant: SentryVariant?,
            extension: SentryPluginExtension,
            cliExecutable: String,
            sentryOrg: String?,
            buildType: String
        ): Provider<SentryTelemetryService> {
            if (extension.telemetry.orNull == false) {
                return project.provider { null }
            }

            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryTelemetryService::class.java),
                SentryTelemetryService::class.java
            ) {
                it.parameters.sentryOrganization.set(sentryOrg)
                it.parameters.dsn.set(extension.telemetryDsn)
                it.parameters.buildType.set(buildType)
                it.parameters.sendTelemetry.set(extension.telemetry)

                if (isExecAvailable()) {
                    val infoResult = runSentryCliInfo(project, variant, cliExecutable, extension)

                    val infoOutput = infoResult.standardOutput.asText.getOrElse("")
                    val isSaas = infoOutput.contains("(?m)Sentry Server: .*sentry.io$".toRegex())
                    it.parameters.saas.set(isSaas)

                    orgRegex.find(infoOutput)?.let { matchResult ->
                        val groupValues = matchResult.groupValues
                        if (groupValues.size > 1) {
                            val defaultOrg = groupValues[1]
                            it.parameters.defaultSentryOrganization.set(defaultOrg)
                        }
                    }

                    val versionResult = runSentryCliVersion(project, cliExecutable)
                    val versionOutput = versionResult.standardOutput.asText.getOrElse("")
                    versionRegex.find(versionOutput)?.let { matchResult ->
                        val groupValues = matchResult.groupValues
                        if (groupValues.size > 1) {
                            val version = groupValues[1]
                            it.parameters.cliVersion.set(version)
                        }
                    }
                }

                val tags = extraTagsFromExtension(project, extension)
                it.parameters.extraTags.set(tags)
            }
        }

        private fun isExecAvailable(): Boolean {
            return GradleVersions.CURRENT >= GradleVersions.VERSION_7_5
        }

        private fun runSentryCliInfo(
            project: Project,
            variant: SentryVariant?,
            cliExecutable: String,
            extension: SentryPluginExtension
        ): ExecOutput {
            return project.providers.exec { exec ->
                exec.isIgnoreExitValue = true
                var args = mutableListOf(cliExecutable)

//                args.add("--log-level=debug")

                extension.url.orNull?.let {
                    args.add("--url")
                    args.add(it)
                }

                args.add("info")

                variant?.let { variantNotNul ->
                    SentryPropertiesFileProvider.getPropertiesFilePath(project, variantNotNul)
                        ?.let {
                            exec.environment("SENTRY_PROPERTIES", File(it))
                        }
                }

                extension.authToken.orNull?.let { authToken ->
                    exec.environment("SENTRY_AUTH_TOKEN", authToken)
                }

                exec.commandLine(args)
            }
        }

        private fun runSentryCliVersion(
            project: Project,
            cliExecutable: String
        ): ExecOutput {
            return project.providers.exec { exec ->
                exec.isIgnoreExitValue = true
                var args = mutableListOf(cliExecutable)
                args.add("--version")
                exec.commandLine(args)
            }
        }

        private fun extraTagsFromExtension(
            project: Project,
            extension: SentryPluginExtension
        ): Map<String, String> {
            val tags = mutableMapOf<String, String>()

            tags.put("SENTRY_debug", extension.debug.get().toString())
            tags.put(
                "SENTRY_includeProguardMapping",
                extension.includeProguardMapping.get().toString()
            )
            tags.put(
                "SENTRY_autoUploadProguardMapping",
                extension.autoUploadProguardMapping.get().toString()
            )
            tags.put("SENTRY_autoUpload", extension.autoUpload.get().toString())
            tags.put("SENTRY_uploadNativeSymbols", extension.uploadNativeSymbols.get().toString())
            tags.put(
                "SENTRY_autoUploadNativeSymbols",
                extension.autoUploadNativeSymbols.get().toString()
            )
            tags.put("SENTRY_includeNativeSources", extension.includeNativeSources.get().toString())
            // TODO PII?
            tags.put(
                "SENTRY_ignoredVariants_set",
                extension.ignoredVariants.get().isNotEmpty().toString()
            )
            // TODO PII?
            tags.put(
                "SENTRY_ignoredBuildTypes_set",
                extension.ignoredBuildTypes.get().isNotEmpty().toString()
            )
            // TODO PII?
            tags.put(
                "SENTRY_ignoredFlavors",
                extension.ignoredFlavors.get().isNotEmpty().toString()
            )
            tags.put(
                "SENTRY_experimentalGuardsquareSupport",
                extension.experimentalGuardsquareSupport.get().toString()
            )
            tags.put(
                "SENTRY_tracing_enabled",
                extension.tracingInstrumentation.enabled.get().toString()
            )
            tags.put(
                "SENTRY_tracing_debug",
                extension.tracingInstrumentation.debug.get().toString()
            )
            tags.put(
                "SENTRY_tracing_forceInstrumentDependencies",
                extension.tracingInstrumentation.forceInstrumentDependencies.get().toString()
            )
            tags.put(
                "SENTRY_tracing_features",
                extension.tracingInstrumentation.features.get().toString()
            )
            tags.put(
                "SENTRY_tracing_logcat_enabled",
                extension.tracingInstrumentation.logcat.enabled.get().toString()
            )
            tags.put(
                "SENTRY_tracing_logcat_minLevel",
                extension.tracingInstrumentation.logcat.minLevel.get().toString()
            )
            tags.put(
                "SENTRY_autoInstallation_enabled",
                extension.autoInstallation.enabled.get().toString()
            )
            tags.put(
                "SENTRY_autoInstallation_sentryVersion",
                extension.autoInstallation.sentryVersion.get().toString()
            )
            tags.put(
                "SENTRY_includeDependenciesReport",
                extension.includeDependenciesReport.get().toString()
            )
            tags.put("SENTRY_includeSourceContext", extension.includeSourceContext.get().toString())
            // TODO PII?
            tags.put(
                "SENTRY_additionalSourceDirsForSourceContext_set",
                extension.additionalSourceDirsForSourceContext.get().isNotEmpty().toString()
            )
            // TODO PII?
            extension.org.orNull?.let { tags.put("SENTRY_org", it) }
            // TODO PII?
            extension.projectName.orNull?.let { tags.put("SENTRY_projectName", it) }

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
