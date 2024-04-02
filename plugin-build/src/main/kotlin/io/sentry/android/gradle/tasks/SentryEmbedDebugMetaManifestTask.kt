package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.PropertiesUtil
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

internal abstract class SentryEmbedDebugMetaManifestTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @get:InputFile
    abstract val inputManifestFile: RegularFileProperty

    @get:OutputFile
    abstract val outputManifestFile: RegularFileProperty

    @TaskAction
    fun addSentryMetadataToAndroidManifest() {
        project.logger.info(
            "EmbedSentryManifestMetadataTask: Embedding Sentry metadata into AndroidManifest.xml"
        )

        val props = Properties()
        props.setProperty("io.sentry.build-tool", "gradle")
        inputFiles.forEach { inputFile ->
            PropertiesUtil.loadMaybe(inputFile)?.let { props.putAll(it) }
        }

        // TODO: properly parse and encode XML
        val rawXml = StringBuilder()
        for (prop in props) {
            val key = prop.key.toString()
            val value = prop.value.toString()
            rawXml.append("<meta-data android:name=\"$key\" android:value=\"$value\" />")
        }

        val inputManifestXml = inputManifestFile.get().asFile.readText(Charsets.UTF_8)
        val outputManifestXml = inputManifestXml.replace(
            "</application>",
            "$rawXml</application>"
        )
        outputManifestFile.get().asFile.writeText(outputManifestXml)
    }

    companion object {
        fun register(
            project: Project,
            taskSuffix: String,
            manifestFile: Provider<RegularFile>,
            inputProperties: List<TaskProvider<out PropertiesFileOutputTask>>
        ): TaskProvider<SentryEmbedDebugMetaManifestTask> {
            val task = project.tasks.register(
                "sentryEmbedDebugMetaManifest$taskSuffix",
                SentryEmbedDebugMetaManifestTask::class.java
            ) { task ->
                // e.g. build/intermediates/merged_manifests/release/AndroidManifest.xml
                task.inputManifestFile.set(manifestFile)

                // input properties files
                task.inputFiles.setFrom(
                    inputProperties.mapNotNull {
                        it.flatMap { task -> task.outputFile }
                    }
                )
                // output: same as input
                task.outputManifestFile.set(manifestFile)
            }

            return task
        }
    }
}
