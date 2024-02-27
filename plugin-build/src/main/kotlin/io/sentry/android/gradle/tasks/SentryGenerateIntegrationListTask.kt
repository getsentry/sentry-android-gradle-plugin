package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.ManifestWriter
import io.sentry.android.gradle.services.SentryModulesService
import io.sentry.android.gradle.util.info
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class SentryGenerateIntegrationListTask : DefaultTask() {

    companion object {
        const val ATTR_INTEGRATIONS = "io.sentry.gradle-plugin-integrations"
    }

    init {
        description = "Writes enabled integrations to AndroidManifest.xml"
    }

    @get:PathSensitive(NONE) // we only care about contents
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @get:Input
    abstract val integrations: SetProperty<String>

    @TaskAction
    fun writeIntegrationListToManifest() {
        logger.info {
            "SentryGenerateIntegrationListTask - outputFile: ${updatedManifest.get()}"
        }
        val integrations = integrations.get()
        val manifestFile = mergedManifest.asFile.get()
        val updatedManifestFile = updatedManifest.asFile.get()

        if (integrations.isNotEmpty()) {
            val manifestWriter = ManifestWriter()
            val integrationsList = integrations
                .toList()
                .sorted()
                .joinToString(",")
            manifestWriter.writeMetaData(
                manifestFile,
                updatedManifestFile,
                ATTR_INTEGRATIONS,
                integrationsList
            )
        } else {
            logger.info {
                "No Integrations present, copying input manifest to output"
            }
            Files.copy(
                manifestFile.toPath(),
                updatedManifestFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}
