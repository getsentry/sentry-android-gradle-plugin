package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.asSentryCliExec
import io.sentry.android.gradle.util.hookWithAssembleTasks
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider

abstract class SentryUploadNativeSymbolsTask : Exec() {

    init {
        description = "Uploads native symbols to Sentry"
    }

    @get:Input
    @get:Optional
    abstract val debug: Property<Boolean>

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:Input
    abstract val autoUploadNativeSymbol: Property<Boolean>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val sentryOrganization: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryProject: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryAuthToken: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryUrl: Property<String>

    @get:Input
    abstract val includeNativeSources: Property<Boolean>

    @get:Internal
    abstract val variantName: Property<String>

    override fun exec() {
        computeCommandLineArgs().let {
            commandLine(it)
            logger.info { "cli args: $it" }
        }
        setSentryPropertiesEnv()
        setSentryAuthTokenEnv()
        super.exec()
    }

    internal fun setSentryPropertiesEnv() {
        val sentryProperties = sentryProperties.orNull
        if (sentryProperties != null) {
            environment("SENTRY_PROPERTIES", sentryProperties)
        } else {
            logger.info { "sentryProperties is null" }
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

    internal fun computeCommandLineArgs(): List<String> {
        val args = mutableListOf(
            cliExecutable.get()
        )

        args.add("debug-files")
        args.add("upload")

        if (debug.getOrElse(false)) {
            args.add("--log-level=debug")
        }

        sentryUrl.orNull?.let {
            args.add("--url")
            args.add(it)
        }

        if (!autoUploadNativeSymbol.get()) {
            args.add("--no-upload")
        }

        sentryOrganization.orNull?.let {
            args.add("--org")
            args.add(it)
        }

        sentryProject.orNull?.let {
            args.add("--project")
            args.add(it)
        }

        val sep = File.separator

        // eg absoluteProjectFolderPath/build/intermediates/merged_native_libs/{variantName}
        // where {variantName} could be debug/release...
        args.add(
            File(
                project.buildDir,
                "intermediates${sep}merged_native_libs${sep}${variantName.get()}"
            ).absolutePath
        )

        // Only include sources if includeNativeSources is enabled, as this is opt-in feature
        if (includeNativeSources.get()) {
            args.add("--include-sources")
        }

        if (Os.isFamily(FAMILY_WINDOWS)) {
            args.add(0, "cmd")
            args.add(1, "/c")
        }
        return args
    }

    companion object {
        fun register(
            project: Project,
            variantName: String,
            debug: Property<Boolean>,
            cliExecutable: String,
            sentryProperties: String?,
            sentryOrg: Provider<String>,
            sentryProject: Provider<String>,
            sentryAuthToken: Property<String>,
            sentryUrl: Property<String>,
            includeNativeSources: Property<Boolean>,
            autoUploadNativeSymbols: Property<Boolean>,
            taskSuffix: String = "",
        ): TaskProvider<SentryUploadNativeSymbolsTask> {
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
                task.variantName.set(variantName)
                task.sentryOrganization.set(sentryOrg)
                task.sentryProject.set(sentryProject)
                task.sentryAuthToken.set(sentryAuthToken)
                task.sentryUrl.set(sentryUrl)
                task.asSentryCliExec()
            }
            return uploadSentryNativeSymbolsTask
        }
    }
}

fun SentryVariant.configureNativeSymbolsTask(
    project: Project,
    extension: SentryPluginExtension,
    cliExecutable: String,
    sentryOrg: String?,
    sentryProject: String?
) {
    if (!isDebuggable && extension.uploadNativeSymbols.get()) {
        val sentryProps = SentryPropertiesFileProvider.getPropertiesFilePath(project, this)
        // Setup the task to upload native symbols task after the assembling task
        val uploadSentryNativeSymbolsTask = SentryUploadNativeSymbolsTask.register(
            project = project,
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
