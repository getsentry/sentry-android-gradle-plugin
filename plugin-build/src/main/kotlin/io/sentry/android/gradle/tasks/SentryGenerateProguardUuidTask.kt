package io.sentry.android.gradle.tasks

import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class SentryGenerateProguardUuidTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a unique build ID to be used " +
            "when uploading the Sentry mapping file"
    }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val outputUuid: Property<UUID>

    @get:Internal
    val outputFile: Provider<RegularFile> get() = outputDirectory.file(
        "sentry-debug-meta.properties"
    )

    @TaskAction
    fun generateProperties() {
        project.logger.info(
            "[sentry] SentryGenerateProguardUuidTask - outputFile: ${outputFile.get()}"
        )

        UUID.randomUUID().also {
            outputUuid.set(it)
            outputFile.get().asFile.parentFile.mkdirs()
            outputFile.get().asFile.writeText("io.sentry.ProguardUuids=$it")
        }
    }
}
