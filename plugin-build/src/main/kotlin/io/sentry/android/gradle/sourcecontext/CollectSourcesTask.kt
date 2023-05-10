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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@CacheableTask
abstract class CollectSourcesTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DirectoryOutputTask() {

    init {
        group = SENTRY_GROUP
        description = "Collects sources into a single directory so they can be bundled together."
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val sourceDirs: ListProperty<ConfigurableFileCollection>

    @TaskAction fun action() {
        workerExecutor.noIsolation().submit(CollectSourcesWorkAction::class.java) {
            it.sourceDirs.set(this@CollectSourcesTask.sourceDirs)
            it.output.set(this@CollectSourcesTask.output)
        }
    }

    interface CollectSourcesParameters : WorkParameters {
        val sourceDirs: ListProperty<ConfigurableFileCollection>
        val output: DirectoryProperty
    }

    abstract class CollectSourcesWorkAction : WorkAction<CollectSourcesParameters> {

        override fun execute() {
            val outDir = parameters.output.getAndDelete()
            SourceCollector().collectSources(outDir, parameters.sourceDirs.get())
        }
    }

    companion object {
        fun register(
            project: Project,
            sourceDirs: Provider<List<ConfigurableFileCollection>>,
            output: Provider<Directory>,
            taskSuffix: String = ""
        ): TaskProvider<CollectSourcesTask> {
            return project.tasks.register("sentryCollectSources${taskSuffix}", CollectSourcesTask::class.java) { task ->
                task.sourceDirs.set(sourceDirs)
                task.output.set(output)
            }
        }
    }
}

internal class SourceCollector {

    fun collectSources(outDir: File, sourceDirs: List<ConfigurableFileCollection>) {
        sourceDirs.forEach { sourceDirCollection ->
            sourceDirCollection.forEach { sourceDir ->
                if (sourceDir.exists()) {
                    SentryPlugin.logger.debug("Collecting sources in ${sourceDir.absolutePath}")
                    sourceDir.walk().forEach { sourceFile ->
                        val relativePath =
                            sourceFile.absolutePath.removePrefix(sourceDir.absolutePath)
                                .removePrefix("/")
                        val targetFile = outDir.resolve(File(relativePath))
                        if (sourceFile.isFile) {
                            SentryPlugin.logger.debug("Copying file ${sourceFile.absolutePath} to ${targetFile.absolutePath}")
                            sourceFile.copyTo(targetFile, true)
                        }
                    }
                } else {
                    SentryPlugin.logger.debug("Skipping source collection in ${sourceDir.absolutePath} as it doesn't exist.")
                }
            }
        }
    }
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
