package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.SentryPluginUtils
import io.sentry.android.gradle.util.info
import java.io.File
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class SentryGenerateProguardUuidTask : DirectoryOutputTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a unique build ID to be used " +
            "when uploading the Sentry mapping file"
    }

    @get:Internal
    val outputFile: Provider<RegularFile> get() = output.file(SENTRY_UUID_OUTPUT)

    @TaskAction
    fun generateProperties() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val uuid = UUID.randomUUID()
        outputFile.get().asFile.writeText("io.sentry.ProguardUuids=$uuid")

        logger.info {
            "SentryGenerateProguardUuidTask - outputFile: $outputFile, uuid: $uuid"
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
                "generateSentryProguardUuid$taskSuffix",
                SentryGenerateProguardUuidTask::class.java
            ) { task ->
                output?.let { task.output.set(it) }
            }
            return generateUuidTask
        }
    }
}
