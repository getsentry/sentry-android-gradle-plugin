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
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryCliInfoValueSource.Params
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SentryCliException
import io.sentry.android.gradle.util.getBuildServiceName
import io.sentry.exception.ExceptionMechanismException
import io.sentry.gradle.common.SentryVariant
import io.sentry.protocol.Mechanism
import io.sentry.protocol.User
import java.io.ByteArrayOutputStream
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

abstract class SentryTelemetryService :
    BuildService<None>,
    BuildOperationListener,
    AutoCloseable {

    private var hub: IHub = NoOpHub.getInstance()
    private var transaction: ITransaction? = null
    private var didAddChildSpans: Boolean = false
    private var started: Boolean = false

    fun start(startParameters: SentryTelemetryServiceParams) {
        if (started) {
            return
        }

        started = true
        try {
            if (startParameters.saas == false) {
                SentryPlugin.logger.info(
                    "Sentry is running against a self hosted instance. Telemetry has been disabled."
                )
                hub = NoOpHub.getInstance()
            } else if (!startParameters.sendTelemetry) {
                SentryPlugin.logger.info("Sentry telemetry has been disabled.")
                hub = NoOpHub.getInstance()
            } else {
                if (!Sentry.isEnabled()) {
                    SentryPlugin.logger.info(
                        "Sentry telemetry is enabled. To disable set `telemetry=false` " +
                            "in the sentry config block."
                    )
                    Sentry.init { options ->
                        options.dsn = startParameters.dsn
                        options.isDebug = true
                        options.isEnablePrettySerializationOutput = false
                        options.tracesSampleRate = 1.0
                        options.release = BuildConfig.Version
                        options.isSendModules = false
                        options.environment = startParameters.buildType
                        options.setTag("SDK_VERSION", BuildConfig.SdkVersion)
                        options.setTag("AGP_VERSION", AgpVersions.CURRENT.toString())
                        options.setTag("BUILD_SYSTEM", "gradle")
                        options.setTag("BUILD_TYPE", startParameters.buildType)
                        options.setTag("GRADLE_VERSION", GradleVersion.current().version)
                        startParameters.cliVersion?.let { options.setTag("SENTRY_CLI_VERSION", it) }

                        startParameters.extraTags.forEach { (key, value) ->
                            options.setTag(
                                key,
                                value
                            )
                        }
                    }
                }
                hub = Sentry.getCurrentHub()
                startRun("gradle build ${startParameters.buildType}")

                hub.configureScope { scope ->
                    scope.user = User().also { user ->
                        startParameters.defaultSentryOrganization.let { user.id = it }
                        startParameters.sentryOrganization.let { user.id = it }
                    }
                }
            }
        } catch (t: Throwable) {
            SentryPlugin.logger.error("Sentry failed to initialize.", t)
        }
    }

    override fun started(descriptor: BuildOperationDescriptor, event: OperationStartEvent) {}

    override fun progress(identifier: OperationIdentifier, event: OperationProgressEvent) {}

    override fun finished(
        buildOperationDescriptor: BuildOperationDescriptor,
        operationFinishEvent: OperationFinishEvent
    ) {
        val details = buildOperationDescriptor.details

        operationFinishEvent.failure?.let { error ->
            // TODO check if Sentry task, otherwise ignore
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
        val isSentryTaskName = (details as? ExecuteTaskBuildOperationDetails)
            ?.let {
                it.task.name.substringAfterLast(":")
                    .contains("sentry", ignoreCase = true)
            } ?: false
        return isSentryTaskName ||
            throwable.stackTrace.any { it.className.startsWith("io.sentry") }
    }

    fun captureError(exception: Throwable, operation: String?) {
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

    fun startRun(transactionName: String) {
        hub.startSession()
        transaction = hub.startTransaction(transactionName, "build", true)
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
            "https://dd1f82ad30a331bd7def2a0dce926c6e@o447951.ingest.sentry.io/4506031723446272"
        val MECHANISM_TYPE: String = "GradleTelemetry"
        private val orgRegex = Regex("""(?m)Default Organization: (.*)$""")

        fun createParameters(
            project: Project,
            variant: SentryVariant?,
            extension: SentryPluginExtension,
            cliExecutable: String,
            sentryOrg: String?,
            buildType: String
        ): SentryTelemetryServiceParams {
            val tags = extraTagsFromExtension(project, extension)
            var isSaas: Boolean? = null
            var defaultSentryOrganization: String? = null

            if (isExecAvailable()) {
                return paramsWithExecAvailable(
                    project,
                    cliExecutable,
                    extension,
                    variant,
                    isSaas,
                    defaultSentryOrganization,
                    sentryOrg,
                    buildType,
                    tags
                )
            } else {
                return SentryTelemetryServiceParams(
                    extension.telemetry.get(),
                    extension.telemetryDsn.get(),
                    sentryOrg,
                    buildType,
                    tags,
                    cliVersion = BuildConfig.CliVersion
                )
            }
        }

        private fun paramsWithExecAvailable(
            project: Project,
            cliExecutable: String,
            extension: SentryPluginExtension,
            variant: SentryVariant?,
            isSaas: Boolean?,
            defaultSentryOrganization: String?,
            sentryOrg: String?,
            buildType: String,
            tags: Map<String, String>
        ): SentryTelemetryServiceParams {
            var isSaas1 = isSaas
            var defaultSentryOrganization1 = defaultSentryOrganization
            val infoOutput = project.providers.of(SentryCliInfoValueSource::class.java) { cliVS ->
                cliVS.parameters.cliExecutable.set(cliExecutable)
                cliVS.parameters.authToken.set(extension.authToken)
                cliVS.parameters.url.set(extension.url)
                variant?.let { v ->
                    cliVS.parameters.propertiesFilePath.set(
                        SentryPropertiesFileProvider.getPropertiesFilePath(project, v)
                    )
                }
            }.get()
            isSaas1 = infoOutput.contains("(?m)Sentry Server: .*sentry.io$".toRegex())

            orgRegex.find(infoOutput)?.let { matchResult ->
                val groupValues = matchResult.groupValues
                if (groupValues.size > 1) {
                    val defaultOrg = groupValues[1]
                    defaultSentryOrganization1 = defaultOrg
                }
            }

            return SentryTelemetryServiceParams(
                extension.telemetry.get(),
                extension.telemetryDsn.get(),
                sentryOrg,
                buildType,
                tags,
                cliVersion = BuildConfig.CliVersion
            )
        }

        fun register(project: Project): Provider<SentryTelemetryService> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryTelemetryService::class.java),
                SentryTelemetryService::class.java
            ) {}
        }

        private fun isExecAvailable(): Boolean {
            return GradleVersions.CURRENT >= GradleVersions.VERSION_7_5
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

abstract class SentryCliInfoValueSource : ValueSource<String, Params> {
    interface Params : ValueSourceParameters {
        @get:Input
        val cliExecutable: Property<String>

        @get:Input
        val propertiesFilePath: Property<String>

        @get:Input
        val url: Property<String>

        @get:Input
        val authToken: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            it.isIgnoreExitValue = true
            val args = mutableListOf(parameters.cliExecutable.get())

            parameters.url.orNull?.let { url ->
                args.add("--url")
                args.add(url)
            }

            args.add("info")

            parameters.propertiesFilePath.orNull?.let { path ->
                it.environment("SENTRY_PROPERTIES", path)
            }

            parameters.authToken.orNull?.let { authToken ->
                it.environment("SENTRY_AUTH_TOKEN", authToken)
            }

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
    val defaultSentryOrganization: String? = null,
    val saas: Boolean? = null,
    val cliVersion: String? = null
)
