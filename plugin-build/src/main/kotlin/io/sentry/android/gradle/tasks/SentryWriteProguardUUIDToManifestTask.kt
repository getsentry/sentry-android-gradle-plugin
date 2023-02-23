package io.sentry.android.gradle.tasks.io.sentry.android.gradle.tasks

import io.sentry.android.gradle.tasks.io.sentry.android.gradle.ManifestWriter
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SentryWriteProguardUUIDToManifestTask : DefaultTask() {

    companion object {
        private const val TAG_PROGUARD_UUID = "io.sentry.ProguardUuids"
    }

    @get:InputFile
    abstract val gitInfoFile: RegularFileProperty

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun writeProguardUUIDToManifest() {
        val uuid = SentryUploadProguardMappingsTask.readUuidFromFile(gitInfoFile.get().asFile)
        val manifestFile = mergedManifest.asFile.get()
        val updatedManifestFile = updatedManifest.asFile.get()
        val manifestWriter = ManifestWriter()
        manifestWriter.writeMetaData(
            manifestFile,
            updatedManifestFile,
            TAG_PROGUARD_UUID,
            uuid
        )
    }
}
