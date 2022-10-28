package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.info
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
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
        logger.info {
            "SentryGenerateProguardUuidTask - outputFile: ${output.get()}"
        }

        UUID.randomUUID().also {
            output.get().asFile.parentFile.mkdirs()
            output.get().asFile.writeText("io.sentry.ProguardUuids=$it")
        }
    }
}
