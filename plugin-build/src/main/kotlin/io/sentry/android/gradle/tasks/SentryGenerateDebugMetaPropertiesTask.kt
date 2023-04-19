package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.info
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class SentryGenerateDebugMetaPropertiesTask : DirectoryOutputTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Combines multiple properties files into sentry-debug-meta.properties"
    }

    @get:Internal
    val outputFile: Provider<RegularFile> get() = output.file(SENTRY_DEBUG_META_PROPERTIES_OUTPUT)

    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @TaskAction
    fun generateProperties() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val debugMetaPropertiesFile = outputFile.get().asFile
        debugMetaPropertiesFile.writeText("") // clear file
        inputFiles.forEach { inputFile ->
            debugMetaPropertiesFile.appendText(inputFile.readText())
            debugMetaPropertiesFile.appendText("\n")
        }
    }

    companion object {
        internal const val SENTRY_DEBUG_META_PROPERTIES_OUTPUT = "sentry-debug-meta.properties"

        fun register(
            project: Project,
            tasksGeneratingProperties: List<TaskProvider<*>>,
            output: Provider<Directory>? = null,
            taskSuffix: String = ""
        ): TaskProvider<SentryGenerateDebugMetaPropertiesTask> {
            val inputFiles: List<Provider<RegularFile>> = tasksGeneratingProperties.mapNotNull {
                val propertiesProducingTask = it as? TaskProvider<PropertiesFileOutputTask>
                propertiesProducingTask?.flatMap { it.outputFile }
            }
            return project.tasks.register(
                "generateSentryDebugMetaProperties$taskSuffix",
                SentryGenerateDebugMetaPropertiesTask::class.java
            ) { task ->
                task.inputFiles.setFrom(inputFiles)
                output?.let { task.output.set(it) }
            }
        }
    }
}
