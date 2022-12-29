package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.SentryPluginUtils
import io.sentry.android.gradle.util.info
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SentryGenerateProguardUuidTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a unique build ID to be used " +
            "when uploading the Sentry mapping file"
    }

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generateProperties() {
        val outputFile = SentryPluginUtils.getAndDeleteFile(output)
        outputFile.parentFile.mkdirs()

        val uuid = UUID.randomUUID()
        outputFile.writeText("io.sentry.ProguardUuids=$uuid")

        logger.info {
            "SentryGenerateProguardUuidTask - outputFile: $outputFile, uuid: $uuid"
        }
    }
}
