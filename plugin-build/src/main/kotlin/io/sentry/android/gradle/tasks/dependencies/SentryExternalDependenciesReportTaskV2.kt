package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.util.SentryPluginUtils
import io.sentry.android.gradle.util.artifactsFor
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class SentryExternalDependenciesReportTaskV2 : DefaultTask() {

    @get:Input
    abstract val includeReport: Property<Boolean>

    init {
        description = "Generates an external dependencies report"

        @Suppress("LeakingThis")
        onlyIf { includeReport.get() }
    }

    @get:Input
    abstract val artifactIds: SetProperty<String>

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun action() {
        val outputDir = SentryPluginUtils.getAndDeleteDir(output)
        outputDir.mkdirs()

        val dependencies = artifactIds.get().toSortedSet()

        val outputFile = File(outputDir, SENTRY_DEPENDENCIES_REPORT_OUTPUT)
        outputFile.writeText(dependencies.joinToString("\n"))
    }

    companion object {
        internal const val SENTRY_DEPENDENCIES_REPORT_OUTPUT = "sentry-external-modules.txt"

        fun register(
            project: Project,
            configurationName: String,
            attributeValueJar: String,
            includeReport: Provider<Boolean>,
            taskSuffix: String = ""
        ): TaskProvider<SentryExternalDependenciesReportTaskV2> {
            return project.tasks.register(
                "collectExternal${taskSuffix}DependenciesForSentry",
                SentryExternalDependenciesReportTaskV2::class.java
            ) {
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
                it.artifactIds.set(artifactIds)
                it.includeReport.set(includeReport)
//                it.output.set(output)
            }
        }
    }
}
