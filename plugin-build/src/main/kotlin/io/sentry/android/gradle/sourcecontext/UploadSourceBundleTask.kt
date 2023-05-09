package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.provider.Property

abstract class UploadSourceBundleTask : Exec() {

    init {
        group = SENTRY_GROUP
        description = "Uploads a Sentry source bundle file."
    }

    @get:InputDirectory
    abstract val sourceBundleDir: DirectoryProperty

    @get:Input
    abstract val debug: Property<Boolean>

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:Input
    abstract val autoUploadSourceBundle: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val sentryOrganization: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryProject: Property<String>

    override fun exec() {
        computeCommandLineArgs().let {
            commandLine(it)
            logger.error("cli args: $it")
        }
        setSentryPropertiesEnv()
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

    internal fun computeCommandLineArgs(): List<String> {
        val args = mutableListOf(cliExecutable.get())

        if (debug.get()) {
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

        if (!autoUploadSourceBundle.get()) {
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
            autoUploadSourceBundle: Property<Boolean>,
            sentryOrg: String?,
            sentryProject: String?,
            taskSuffix: String = ""
        ): TaskProvider<UploadSourceBundleTask> {
            return project.tasks.register("sentryUploadSourceBundle${taskSuffix}", UploadSourceBundleTask::class.java) { task ->
                task.debug.set(debug)
                task.sentryOrganization.set(sentryOrg)
                task.sentryProject.set(sentryProject)
                task.sourceBundleDir.set(bundleSourcesTask.flatMap { it.output })
                task.cliExecutable.set(cliExecutable)
                task.autoUploadSourceBundle.set(autoUploadSourceBundle)
                SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)?.let {
                    task.sentryProperties.set(File(it))
                }
            }
        }
    }
}
