package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory.SENTRY_DEPENDENCIES_REPORT_OUTPUT
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SentryPluginUtils
import io.sentry.android.gradle.util.artifactsFor
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class SentryExternalDependenciesReportTask : DirectoryOutputTask() {

    @get:Input
    abstract val includeReport: Property<Boolean>

    init {
        description = "Generates an external dependencies report"

        if (GradleVersions.CURRENT >= GradleVersions.VERSION_7_4) {
            @Suppress("LeakingThis")
            notCompatibleWithConfigurationCache("Cannot serialize Configurations")
        }
        @Suppress("LeakingThis")
        onlyIf { includeReport.get() }
    }

    @Transient
    private lateinit var runtimeConfiguration: Configuration

    fun setRuntimeConfiguration(configuration: Configuration) {
        runtimeConfiguration = configuration
    }

    @get:Input
    abstract val attributeValueJar: Property<String>

    // this is a proper input, so our task gets triggered whenever the dependency set changes
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFiles
    fun getRuntimeClasspath(): FileCollection = runtimeConfiguration.artifactsFor(
        attributeValueJar.get()
    ).artifactFiles

    @TaskAction
    fun action() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val dependencies = runtimeConfiguration
            .incoming
            .resolutionResult
            .allComponents
            // we're only interested in external deps
            .filter { it.id is ModuleComponentIdentifier }
            // and those that have proper version defined (e.g. flat jars don't have it)
            .filter { it.moduleVersion?.version?.isNotEmpty() == true }
            .map { it.id.displayName }
            .toSortedSet()

        val outputFile = File(outputDir, SENTRY_DEPENDENCIES_REPORT_OUTPUT)
        outputFile.writeText(dependencies.joinToString("\n"))
    }

    companion object {
        fun register(
            project: Project,
            configurationName: String,
            attributeValueJar: String,
            output: Provider<Directory>?,
            includeReport: Provider<Boolean>,
            taskSuffix: String = ""
        ): TaskProvider<SentryExternalDependenciesReportTask> {
            return project.tasks.register(
                "collectExternal${taskSuffix}DependenciesForSentry",
                SentryExternalDependenciesReportTask::class.java
            ) { task ->
                task.includeReport.set(includeReport)
                task.attributeValueJar.set(attributeValueJar)
                task.setRuntimeConfiguration(
                    project.configurations.getByName(configurationName)
                )
                output?.let { task.output.set(it) }
            }
        }
    }
}
