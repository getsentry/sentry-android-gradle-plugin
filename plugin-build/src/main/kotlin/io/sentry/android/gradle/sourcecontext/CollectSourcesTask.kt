package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.tasks.DirectoryOutputTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

@CacheableTask
abstract class CollectSourcesTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val layout: ProjectLayout
) : DirectoryOutputTask() {

    init {
        group = SENTRY_GROUP
        description = "Collects sources into a single directory so they can be bundled together."
    }

    /** The Groovy source of the current project. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction fun action() {
        workerExecutor.noIsolation().submit(CollectSourcesWorkAction::class.java) {
            it.projectDir.set(layout.projectDirectory)
            it.sourceFiles.from(this@CollectSourcesTask.sourceFiles)
            it.output.set(this@CollectSourcesTask.output)
        }
    }

    interface CollectSourcesParameters : WorkParameters {
        val projectDir: DirectoryProperty
        val sourceFiles: ConfigurableFileCollection
        val output: DirectoryProperty
    }

    abstract class CollectSourcesWorkAction : WorkAction<CollectSourcesParameters> {

        override fun execute() {
            val outDir = parameters.output.getAndDelete()

            println(parameters.sourceFiles.asFileTree.map { it.absolutePath })

            val collectedSources = SourceCollector(
                projectDir = parameters.projectDir.get().asFile,
                sourceFiles = parameters.sourceFiles
            ).collect()

            collectedSources.forEach { collectedSource ->
                println(collectedSource)
                val targetFile = outDir.resolve(File(collectedSource.relativePath))
                collectedSource.file.copyTo(targetFile, true)
            }
        }
    }

    companion object {
        fun register(
            project: Project,
            sourceFiles: List<File>,
            output: Provider<Directory>,
            taskSuffix: String = ""
        ): TaskProvider<CollectSourcesTask> {
//        val oldAGPExtension = project.extensions.getByType(AppExtension::class.java)
            return project.tasks.register("sentryCollectSources${taskSuffix}", CollectSourcesTask::class.java) { task ->
//            task.sourceFiles.setFrom(oldAGPExtension.sourceSets.flatMap { it.java.getSourceFiles() })
                task.sourceFiles.setFrom(sourceFiles)
                task.output.set(output)
            }
        }
    }
}

private class SourceCollector(
    private val projectDir: File,
    private val sourceFiles: ConfigurableFileCollection
) {

    fun collect(): Set<SourceFile> {
        val destination = sortedSetOf<SourceFile>()
        sourceFiles.mapTo(destination) {
            val rel = relativePathInSrcDir(relativize(it))
            println("flattening ${it.absolutePath}")
            SourceFile(
                file = it,
                relativePath = rel
            )
        }
        return destination
    }

    private fun relativize(file: File) = file.toRelativeString(projectDir)

    private fun relativePathInSrcDir(relativePath: String): String {
        return Paths.get(relativePath)
            // Hack to drop e.g. `src/main/java`. Would be better if a FileTree exposed that info.
            .drop(3)
            .joinToString(separator = File.separator)
    }
}

internal data class SourceFile(
    val file: File,
    val relativePath: String
) : Comparable<SourceFile> {

    override fun compareTo(other: SourceFile): Int = relativePath.compareTo(other.relativePath)
}

internal fun DirectoryProperty.getAndDelete(): File {
    val file = get().asFile
    if (file.isDirectory) {
        file.deleteRecursively()
    } else {
        file.delete()
    }
    return file
}

internal fun RegularFileProperty.getAndDelete(): File {
    val file = get().asFile
    if (file.isDirectory) {
        file.deleteRecursively()
    } else {
        file.delete()
    }
    return file
}
