package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.info
import java.util.Properties
import java.util.UUID
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class SentryGenerateProguardUuidTask : PropertiesFileOutputTask() {

    init {
        description = "Generates a unique build ID to be used " +
            "when uploading the Sentry mapping file"
    }

    @get:Internal
    override val outputFile: Provider<RegularFile> get() = output.file(SENTRY_UUID_OUTPUT)

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val sourceDirs: ConfigurableFileCollection

    @TaskAction
    fun generateProperties() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val uuid = UUID.randomUUID()

        val props = Properties().also {
            it.setProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY, uuid.toString())
        }

        outputFile.get().asFile.writer().use { writer ->
            props.store(writer, "")
        }

        logger.info {
            "SentryGenerateProguardUuidTask - outputFile: $outputFile, uuid: $uuid"
        }
    }

    companion object {
        internal const val SENTRY_UUID_OUTPUT = "sentry-proguard-uuid.properties"
        const val SENTRY_PROGUARD_MAPPING_UUID_PROPERTY = "io.sentry.ProguardUuids"

        fun register(
            project: Project,
            extension: SentryPluginExtension,
            sentryTelemetryProvider: Provider<SentryTelemetryService>?,
            output: Provider<Directory>? = null,
            sourceFiles: Provider<FileCollection>?,
            taskSuffix: String = ""
        ): TaskProvider<SentryGenerateProguardUuidTask> {
            val generateUuidTask = project.tasks.register(
                "generateSentryProguardUuid$taskSuffix",
                SentryGenerateProguardUuidTask::class.java
            ) { task ->
                output?.let { task.output.set(it) }
                task.withSentryTelemetry(extension, sentryTelemetryProvider)
                task.sourceDirs.setFrom(sourceFiles)
            }
            return generateUuidTask
        }
    }
}
