package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.asSentryCliExec
import io.sentry.android.gradle.util.hookWithAssembleTasks
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider

abstract class SentryUploadNativeSymbolsTask : SentryCliExecTask() {

    init {
        description = "Uploads native symbols to Sentry"
    }

    @get:Input
    abstract val autoUploadNativeSymbol: Property<Boolean>

    @get:Input
    abstract val includeNativeSources: Property<Boolean>

    @get:InputFiles
    abstract val nativeLibsDir: ConfigurableFileCollection

    override fun getArguments(args: MutableList<String>) {
        args.add("debug-files")
        args.add("upload")

        if (!autoUploadNativeSymbol.get()) {
            args.add("--no-upload")
        }

        args.add(nativeLibsDir.asPath)

        // Only include sources if includeNativeSources is enabled, as this is opt-in feature
        if (includeNativeSources.get()) {
            args.add("--include-sources")
        }
    }

    companion object {
        fun register(
            project: Project,
            extension: SentryPluginExtension,
            sentryTelemetryProvider: Provider<SentryTelemetryService>,
            variantName: String,
            debug: Property<Boolean>,
            cliExecutable: Provider<String>,
            sentryProperties: String?,
            sentryOrg: Provider<String>,
            sentryProject: Provider<String>,
            sentryAuthToken: Property<String>,
            sentryUrl: Property<String>,
            includeNativeSources: Property<Boolean>,
            autoUploadNativeSymbols: Property<Boolean>,
            taskSuffix: String = "",
        ): TaskProvider<SentryUploadNativeSymbolsTask> {
            val nativeLibsDir = File(
                project.buildDir,
                "intermediates${File.separator}merged_native_libs${File.separator}${variantName}"
            )
            val uploadSentryNativeSymbolsTask = project.tasks.register(
                "uploadSentryNativeSymbolsFor$taskSuffix",
                SentryUploadNativeSymbolsTask::class.java
            ) { task ->
                task.workingDir(project.rootDir)
                task.debug.set(debug)
                task.autoUploadNativeSymbol.set(autoUploadNativeSymbols)
                task.cliExecutable.set(cliExecutable)
                task.sentryProperties.set(sentryProperties?.let { file -> project.file(file) })
                task.includeNativeSources.set(includeNativeSources)
                task.sentryOrganization.set(sentryOrg)
                task.sentryProject.set(sentryProject)
                task.sentryAuthToken.set(sentryAuthToken)
                task.sentryUrl.set(sentryUrl)
                task.sentryTelemetryService.set(sentryTelemetryProvider)
                task.nativeLibsDir.setFrom(nativeLibsDir)
                task.asSentryCliExec()
                task.withSentryTelemetry(extension, sentryTelemetryProvider)
            }
            return uploadSentryNativeSymbolsTask
        }
    }
}

fun SentryVariant.configureNativeSymbolsTask(
    project: Project,
    extension: SentryPluginExtension,
    sentryTelemetryProvider: Provider<SentryTelemetryService>,
    cliExecutable: Provider<String>,
    sentryOrg: String?,
    sentryProject: String?
) {
    if (!isDebuggable && extension.uploadNativeSymbols.get()) {
        val sentryProps = SentryPropertiesFileProvider.getPropertiesFilePath(project, this)
        // Setup the task to upload native symbols task after the assembling task
        val uploadSentryNativeSymbolsTask = SentryUploadNativeSymbolsTask.register(
            project = project,
            extension = extension,
            sentryTelemetryProvider = sentryTelemetryProvider,
            variantName = name,
            debug = extension.debug,
            cliExecutable = cliExecutable,
            sentryProperties = sentryProps,
            autoUploadNativeSymbols = extension.autoUploadNativeSymbols,
            includeNativeSources = extension.includeNativeSources,
            sentryOrg = sentryOrg?.let { project.provider { it } } ?: extension.org,
            sentryProject = sentryProject?.let { project.provider { it } }
                ?: extension.projectName,
            sentryAuthToken = extension.authToken,
            sentryUrl = extension.url,
            taskSuffix = name.capitalized
        )
        uploadSentryNativeSymbolsTask.hookWithAssembleTasks(project, this)
    } else {
        project.logger.info { "uploadSentryNativeSymbols won't be executed" }
    }
}
