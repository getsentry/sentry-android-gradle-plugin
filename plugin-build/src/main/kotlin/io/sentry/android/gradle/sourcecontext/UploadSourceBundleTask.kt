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
    abstract val cliExecutable: Property<String>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:Input
    abstract val autoUploadSourceBundle: Property<Boolean>

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
        val args = mutableListOf(
            cliExecutable.get(),
            "--log-level=debug",
            "debug-files",
            "upload"
        )
        args.add("--type=jvm")

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
            cliExecutable: String,
            autoUploadSourceBundle: Property<Boolean>,
            taskSuffix: String
        ): TaskProvider<UploadSourceBundleTask> {
            return project.tasks.register("sentryUploadSourceBundle${taskSuffix}", UploadSourceBundleTask::class.java) { task ->
                task.dependsOn(bundleSourcesTask)

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
