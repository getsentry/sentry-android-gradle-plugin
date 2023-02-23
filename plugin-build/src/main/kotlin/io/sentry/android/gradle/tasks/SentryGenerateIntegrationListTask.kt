package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.ManifestWriter
import io.sentry.android.gradle.util.info
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SentryGenerateIntegrationListTask : DefaultTask() {

    companion object {
        private const val TAG_INTEGRATIONS = "io.sentry.integrations"
    }

    init {
        outputs.upToDateWhen { false }
        description = "Writes enabled integrations to AndroidManifest.xml"
    }

    @get:Input
    abstract val integrations: ListProperty<String>

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun writeIntegrationListToManifest() {
        logger.info {
            "SentryGenerateIntegrationListTask - outputFile: ${updatedManifest.get()}"
        }
        val manifestFile = mergedManifest.asFile.get()
        val updatedManifestFile = updatedManifest.asFile.get()

        if (integrations.getOrElse(emptyList()).isNotEmpty()) {
            val manifestWriter = ManifestWriter()
            val integrationsList = integrations.get().joinToString(",")
            manifestWriter.writeMetaData(
                manifestFile,
                updatedManifestFile,
                TAG_INTEGRATIONS,
                integrationsList
            )
        }
    }
}
