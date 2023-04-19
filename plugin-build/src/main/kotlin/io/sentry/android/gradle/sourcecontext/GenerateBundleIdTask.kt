package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.tasks.DirectoryOutputTask
import io.sentry.android.gradle.util.info
import java.util.UUID
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class GenerateBundleIdTask : DirectoryOutputTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a unique build ID to be used " +
            "when bundling sources for upload to Sentry"
    }

    @get:Internal
    val outputFile: Provider<RegularFile> get() = output.file(SENTRY_BUNDLE_ID_OUTPUT)

    @TaskAction
    fun generateProperties() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val debugId = UUID.randomUUID()
        outputFile.get().asFile.writeText("${SENTRY_BUNDLE_ID_PROPERTY}=$debugId")

        logger.info {
            "GenerateSourceBundleIdTask - outputFile: $outputFile, debugId: $debugId"
        }
    }

    companion object {
        internal const val SENTRY_BUNDLE_ID_OUTPUT = "sentry-debug-meta.properties"
        const val SENTRY_BUNDLE_ID_PROPERTY = "sentry-debug-meta.properties"

        fun register(
            project: Project,
            output: Provider<Directory>? = null,
            taskSuffix: String = ""
        ): TaskProvider<GenerateBundleIdTask> {
            val generateBundleIdTask = project.tasks.register(
                taskName(taskSuffix),
                GenerateBundleIdTask::class.java
            ) { task ->
                output?.let { task.output.set(it) }
            }
            return generateBundleIdTask
        }

        fun taskName(taskSuffix: String) = "generateSentryBundleId$taskSuffix"
    }
}
