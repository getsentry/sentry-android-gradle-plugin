package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.SentryCliExecTask
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.asSentryCliExec
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskProvider

abstract class UploadSourceBundleTask : SentryCliExecTask() {

    init {
        group = SENTRY_GROUP
        description = "Uploads a Sentry source bundle file."

        @Suppress("LeakingThis")
        onlyIf {
            includeSourceContext.getOrElse(false) &&
                !sourceBundleDir.asFileTree.isEmpty
        }

        // Allows gradle to consider this task up-to-date if the inputs haven't changed
        // As this task does not have any outputs, it will always be considered to be out-of-date otherwise
        // More info here https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:task_outcomes
        // and https://docs.gradle.org/current/userguide/incremental_build.html#sec:custom_up_to_date_logic
        outputs.upToDateWhen { true }
    }

    @get:Input
    abstract val includeSourceContext: Property<Boolean>

    @get:InputDirectory
    abstract val sourceBundleDir: DirectoryProperty

    @get:Input
    abstract val autoUploadSourceContext: Property<Boolean>

    override fun getArguments(args: MutableList<String>) {
        args.add("debug-files")
        args.add("upload")
        args.add("--type=jvm")

        if (!autoUploadSourceContext.get()) {
            args.add("--no-upload")
        }

        args.add(sourceBundleDir.get().asFile.absolutePath)
    }

    companion object {
        fun register(
            project: Project,
            extension: SentryPluginExtension,
            sentryTelemetryProvider: Provider<SentryTelemetryService>?,
            variant: SentryVariant,
            bundleSourcesTask: TaskProvider<BundleSourcesTask>,
            debug: Property<Boolean>,
            cliExecutable: Provider<String>,
            autoUploadSourceContext: Property<Boolean>,
            sentryOrg: Provider<String>,
            sentryProject: Provider<String>,
            sentryAuthToken: Property<String>,
            sentryUrl: Property<String>,
            includeSourceContext: Property<Boolean>,
            taskSuffix: String = ""
        ): TaskProvider<UploadSourceBundleTask> {
            return project.tasks.register(
                "sentryUploadSourceBundle$taskSuffix",
                UploadSourceBundleTask::class.java
            ) { task ->
                task.debug.set(debug)
                task.sentryOrganization.set(sentryOrg)
                task.sentryProject.set(sentryProject)
                task.sentryAuthToken.set(sentryAuthToken)
                task.sentryUrl.set(sentryUrl)
                task.sourceBundleDir.set(bundleSourcesTask.flatMap { it.output })
                task.cliExecutable.set(cliExecutable)
                task.buildDirectory.set(project.layout.buildDirectory.asFile)
                task.autoUploadSourceContext.set(autoUploadSourceContext)
                SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)?.let {
                    task.sentryProperties.set(File(it))
                }
                task.includeSourceContext.set(includeSourceContext)
                sentryTelemetryProvider?.let { task.sentryTelemetryService.set(it) }
                task.asSentryCliExec()
                task.withSentryTelemetry(extension, sentryTelemetryProvider)
            }
        }
    }
}
