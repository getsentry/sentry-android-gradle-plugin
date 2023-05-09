package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPlugin
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
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
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

            val collectedSources = SourceCollector(
                projectDir = parameters.projectDir.get().asFile,
                sourceFiles = parameters.sourceFiles
            ).collect()

            collectedSources.forEach { collectedSource ->
                if (collectedSource.file.exists()) {
                    SentryPlugin.logger.debug("Collecting sources in ${collectedSource.file.absolutePath}")
                    collectedSource.file.walk().forEach { file ->
                        val relativePath = file.absolutePath.removePrefix(collectedSource.file.absolutePath).removePrefix("/")
                        val targetFile = outDir.resolve(File(relativePath))
                        if (file.isFile) {
                            SentryPlugin.logger.debug("Copying file ${file.absolutePath} to ${targetFile.absolutePath}")
                            file.copyTo(targetFile, true)
                        }
                    }
                } else {
                    SentryPlugin.logger.debug("Skipping source collection in ${collectedSource.file.absolutePath} as it doesn't exist.")
                }
            }
        }
    }

    companion object {
        fun register(
            project: Project,
            sourceFiles: ConfigurableFileCollection,
            output: Provider<Directory>,
            taskSuffix: String = ""
        ): TaskProvider<CollectSourcesTask> {
            return project.tasks.register("sentryCollectSources${taskSuffix}", CollectSourcesTask::class.java) { task ->
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
