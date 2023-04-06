package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.ManifestWriter
import io.sentry.android.gradle.util.info
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SentryWriteProguardUUIDToManifestTask : DefaultTask() {

    companion object {
        const val ATTR_PROGUARD_UUID = "io.sentry.proguard-uuid"
    }

    @get:InputFile
    @get:Optional
    abstract val proguardUUIDFile: RegularFileProperty

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun writeProguardUUIDToManifest() {
        val manifestFile = mergedManifest.asFile.get()
        val updatedManifestFile = updatedManifest.asFile.get()
        val uuidFile = proguardUUIDFile.orNull
        if (uuidFile == null) {
            logger.info {
                "No UUID file present, copying input manifest to output"
            }
            Files.copy(
                manifestFile.toPath(),
                updatedManifestFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } else {
            val uuid = SentryUploadProguardMappingsTask.readUuidFromFile(uuidFile.asFile)
            val manifestWriter = ManifestWriter()
            manifestWriter.writeMetaData(
                manifestFile,
                updatedManifestFile,
                ATTR_PROGUARD_UUID,
                uuid
            )
        }
    }
}
