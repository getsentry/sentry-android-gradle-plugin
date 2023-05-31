package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider

abstract class UploadSourceBundleTask : Exec() {

    init {
        group = SENTRY_GROUP
        description = "Uploads a Sentry source bundle file."
    }

    @get:InputDirectory
    abstract val sourceBundleDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val debug: Property<Boolean>

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:Input
    abstract val autoUploadSourceContext: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val sentryOrganization: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryProject: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryAuthToken: Property<String>

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
        val args = mutableListOf(cliExecutable.get())

        if (debug.getOrElse(false)) {
            args.add("--log-level=debug")
        }

        args.add("debug-files")
        args.add("upload")
        args.add("--type=jvm")

        sentryOrganization.orNull?.let {
            args.add("--org")
            args.add(it)
        }

        sentryProject.orNull?.let {
            args.add("--project")
            args.add(it)
        }

        if (!autoUploadSourceContext.get()) {
            args.add("--no-upload")
        }

        args.add(sourceBundleDir.get().asFile.absolutePath)

        return args
    }

    companion object {
        fun register(
            project: Project,
            variant: AndroidVariant,
            bundleSourcesTask: TaskProvider<BundleSourcesTask>,
            debug: Property<Boolean>,
            cliExecutable: String,
            autoUploadSourceContext: Property<Boolean>,
            sentryOrg: String?,
            sentryProject: String?,
            sentryAuthToken: Property<String>,
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
                task.sourceBundleDir.set(bundleSourcesTask.flatMap { it.output })
                task.cliExecutable.set(cliExecutable)
                task.autoUploadSourceContext.set(autoUploadSourceContext)
                SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)?.let {
                    task.sentryProperties.set(File(it))
                }
                task.onlyIf { !task.sourceBundleDir.asFileTree.isEmpty }
            }
        }
    }
}
