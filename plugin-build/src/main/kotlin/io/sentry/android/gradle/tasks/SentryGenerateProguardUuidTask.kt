package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.info
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class SentryGenerateProguardUuidTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a unique build ID to be used " +
            "when uploading the Sentry mapping file"
    }

    @get:OutputDirectory
    @get:Optional
    abstract val output: DirectoryProperty

    @get:Internal
    val outputFile: Provider<RegularFile> get() = output.file(SENTRY_UUID_OUTPUT)

    @TaskAction
    fun generateProperties() {
        val outputDir = output.orNull?.asFile

        if (outputDir != null) {
            outputDir.mkdirs()

            val uuid = UUID.randomUUID()
            outputFile.get().asFile.writeText("io.sentry.ProguardUuids=$uuid")

            logger.info {
                "SentryGenerateProguardUuidTask - outputFile: $outputFile, uuid: $uuid"
            }
        } else {
            logger.info {
                "Not generating..."
            }
        }
    }

    companion object {
        internal const val SENTRY_UUID_OUTPUT = "sentry-debug-meta.properties"

        fun register(
            project: Project,
            output: Provider<Directory>? = null,
            taskSuffix: String = ""
        ): TaskProvider<SentryGenerateProguardUuidTask> {
            val generateUuidTask = project.tasks.register(
                taskName(taskSuffix),
                SentryGenerateProguardUuidTask::class.java
            ) { task ->
                output?.let { task.output.set(it) }
            }
            return generateUuidTask
        }

        fun taskName(taskSuffix: String = "") = "generateSentryProguardUuid$taskSuffix"
    }
}
