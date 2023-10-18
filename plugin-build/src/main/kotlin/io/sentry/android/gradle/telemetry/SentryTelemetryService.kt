package io.sentry.android.gradle.telemetry

import io.sentry.BuildConfig
import io.sentry.DsnUtil
import io.sentry.HubAdapter
import io.sentry.IHub
import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.NoOpHub
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.getBuildServiceName
import io.sentry.protocol.User
import java.util.UUID
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.execution.RunRootBuildWorkBuildOperationType
import org.gradle.execution.taskgraph.NotifyTaskGraphWhenReadyBuildOperationType
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent
import org.gradle.util.GradleVersion

abstract class SentryTelemetryService : BuildService<SentryTelemetryService.Params>,
    BuildOperationListener,
    AutoCloseable {
    interface Params : BuildServiceParameters {
        @get:Input
        val sendTelemetry: Property<Boolean>

        @get:Input
        val dsn: Property<String>

        @get:Input
        val orgId: Property<String>
    }

    private val hub: IHub
    private var transaction: ITransaction? = null
    private var configPhaseSpan: ISpan? = null
    private var executionPhaseSpan: ISpan? = null
    private var didAddChildSpans: Boolean = false

    init {
        // TODO duplicate transaction SentryPlugin and SentryJvmPlugin
        // TODO config phase errors are not reported because this Service is never initialized
        // TODO run sentry-cli info and grep for "Sentry Server: https://sentry.io"
        // TODO check if DSN is SAAS
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> init service ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
        if (parameters.sendTelemetry.get()) {
            if (!Sentry.isEnabled()) {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> init sentry ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
                Sentry.init { options ->
                    options.dsn = parameters.dsn.get()
                    options.isDebug = true
                    options.tracesSampleRate = 1.0
                    options.release = BuildConfig.Version
                    options.isSendModules = false
                    options.setTag("SDK_VERSION", BuildConfig.SdkVersion)
                    options.setTag("AGP_VERSION", BuildConfig.AgpVersion)
                    options.setTag("BUILD_SYSTEM", "gradle")
                    options.setTag("GRADLE_VERSION", GradleVersion.current().toString())
                    // TODO CLI version
                }
            } else {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> sentry already initialized ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
            }
//        hub = HubAdapter.getInstance()
            hub = Sentry.getCurrentHub()
            startRun("gradle build TODO")

            configPhaseSpan = hub.span?.startChild("configuration phase")
            // TODO extract orgId from DSN
            hub.configureScope { scope ->
                scope.user = User().also { user ->
                    parameters.orgId.orNull?.let { user.id = it }
                }
            }
        } else {
            println("Telemetry has been disabled")
            hub = NoOpHub.getInstance()
        }
    }

    override fun started(descriptor: BuildOperationDescriptor, event: OperationStartEvent) {}

    override fun progress(identifier: OperationIdentifier, event: OperationProgressEvent) {}

    override fun finished(buildOperationDescriptor: BuildOperationDescriptor, operationFinishEvent: OperationFinishEvent) {
        if (buildOperationDescriptor.details is NotifyTaskGraphWhenReadyBuildOperationType.Details) {
            configPhaseSpan?.finish()
            executionPhaseSpan = hub.span?.startChild("execution phase")
        }

        operationFinishEvent.failure?.let { error ->
            captureError("build failed with ${error.javaClass}")
            transaction?.status = SpanStatus.UNKNOWN_ERROR
        }

        if (buildOperationDescriptor.details is RunRootBuildWorkBuildOperationType.Details) {
            executionPhaseSpan?.finish()
            endRun()
        }
        println("finished ${buildOperationDescriptor} - ${operationFinishEvent.result} - ${operationFinishEvent.failure}")

        // ? start transaction and config phase span
        // on task graph ready (NotifyTaskGraphWhenReadyBuildOperationType) end config phase span and start build phase span
        // RunRootBuildWorkBuildOperationType end build phase span, end trasnsaction

        // failure != null captureError
    }

    // A public method for tasks to use
    fun captureError(message: String) {
        println("capturing error $message with Sentry classloader ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
        hub.captureMessage(message, SentryLevel.ERROR)
    }

    fun startRun(operaton: String) {
        hub.startSession()
        transaction = hub.startTransaction("gradle run", "op1", true)
        println("started transaction ${transaction?.eventId} with classloader ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
    }

    fun endRun() {
        println("ending transaction ${transaction?.eventId} with classloader ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
        if (didAddChildSpans) {
            transaction?.finish()
            hub.endSession()
        }
    }

    // TODO get trace and add it to sentry-cli as env var
//    fun get

    fun captureTask(operation: String, callback: () -> Unit) {
        val span = hub.span?.startChild(operation)
        didAddChildSpans = true
        hub.setTag("step", operation)
        try {
            callback()
            span?.status = SpanStatus.OK
        } catch (t: Throwable) {
            // TODO how specific can we get without risking PII?
            captureError("$operation failed with ${t.javaClass}")
            span?.status = SpanStatus.UNKNOWN_ERROR
        } finally {
            span?.finish()
        }
    }

    override fun close() {
        println("close called on classloader ${Sentry::class.java.classLoader} and thread ${Thread.currentThread().id}")
        if (transaction?.isFinished == false) {
            endRun()
        }
        Sentry.close()
    }

    companion object {
        fun register(
            project: Project,
            extension: SentryPluginExtension,
            dsn: String,
            orgId: String?
        ): Provider<SentryTelemetryService> {
            println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> registering service maybe")
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryTelemetryService::class.java),
                SentryTelemetryService::class.java
            ) {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> registering service")
                it.parameters.sendTelemetry.set(extension.telemetry)
                it.parameters.dsn.set(dsn)
                it.parameters.orgId.set(orgId)
            }
        }
    }
}
