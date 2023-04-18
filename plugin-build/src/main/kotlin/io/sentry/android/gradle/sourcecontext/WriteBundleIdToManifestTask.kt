package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.ManifestWriter
import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class WriteBundleIdToManifestTask : DefaultTask() {

    @get:InputFile
    @get:Optional
    abstract val bundleIdFile: RegularFileProperty

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun writeProguardUUIDToManifest() {
        val manifestFile = mergedManifest.asFile.get()
        val updatedManifestFile = updatedManifest.asFile.get()
        val idFile = bundleIdFile.orNull
        if (idFile == null) {
            logger.info {
                "No bundle ID file present, copying input manifest to output"
            }
            Files.copy(
                manifestFile.toPath(),
                updatedManifestFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } else {
            val bundleId = BundleSourcesTask.readBundleIdFromFile(idFile.asFile)
            val manifestWriter = ManifestWriter()
            manifestWriter.writeMetaData(
                manifestFile,
                updatedManifestFile,
                ATTR_BUNDLE_IDS,
                bundleId
            )
        }
    }

    companion object {
        const val ATTR_BUNDLE_IDS = "io.sentry.bundle-ids"

        fun register(
            project: Project,
            generateBundleIdTask: TaskProvider<GenerateBundleIdTask>,
            taskSuffix: String
        ): TaskProvider<WriteBundleIdToManifestTask> {
            return project.tasks.register("sentryWriteBundleIdToManifest${taskSuffix}", WriteBundleIdToManifestTask::class.java) { task ->
                task.dependsOn(generateBundleIdTask)

                task.bundleIdFile.set(generateBundleIdTask.flatMap { it.outputFile })
            }
        }
    }
}
