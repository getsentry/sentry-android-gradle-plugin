package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory.SENTRY_DEPENDENCIES_REPORT_OUTPUT
import io.sentry.android.gradle.util.artifactsFor
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class SentryExternalDependenciesReportTaskV2 : DirectoryOutputTask() {

    @get:Input
    abstract val includeReport: Property<Boolean>

    init {
        description = "Generates an external dependencies report"

        @Suppress("LeakingThis")
        onlyIf { includeReport.get() }
    }

    @get:Input
    abstract val artifactIds: SetProperty<String>

    @TaskAction
    fun action() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val dependencies = artifactIds.get().toSortedSet()

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
        ): TaskProvider<SentryExternalDependenciesReportTaskV2> {
            return project.tasks.register(
                "collectExternal${taskSuffix}DependenciesForSentry",
                SentryExternalDependenciesReportTaskV2::class.java
            ) { task ->
                val configuration = project.configurations.getByName(configurationName)
                val artifacts = configuration.artifactsFor(attributeValueJar).resolvedArtifacts
                val artifactIds = artifacts.map { list ->
                    list.map { artifact -> artifact.id.componentIdentifier }
                        // we're only interested in external deps
                        .filterIsInstance<ModuleComponentIdentifier>()
                        // and those that have proper version defined (e.g. flat jars don't have it)
                        .filter { id -> id.version.isNotEmpty() }
                        .map { id -> id.displayName }
                }
                task.artifactIds.set(artifactIds)
                task.includeReport.set(includeReport)
                output?.let { task.output.set(it) }
            }
        }
    }
}
