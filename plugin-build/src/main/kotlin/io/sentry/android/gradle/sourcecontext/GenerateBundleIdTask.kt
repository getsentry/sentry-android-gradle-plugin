package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.tasks.PropertiesFileOutputTask
import io.sentry.android.gradle.util.info
import java.util.Properties
import java.util.UUID
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class GenerateBundleIdTask : PropertiesFileOutputTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a unique build ID to be used " +
            "when bundling sources for upload to Sentry"
    }

    @get:Internal
    override val outputFile: Provider<RegularFile> get() = output.file(SENTRY_BUNDLE_ID_OUTPUT)

    @TaskAction
    fun generateProperties() {
        val outputDir = output.get().asFile
        outputDir.mkdirs()

        val debugId = UUID.randomUUID()

        val props = Properties().also {
            it.setProperty(SENTRY_BUNDLE_ID_PROPERTY, debugId.toString())
        }

        outputFile.get().asFile.writer().use { writer ->
            props.store(writer, "")
        }

        logger.info {
            "GenerateSourceBundleIdTask - outputFile: $outputFile, debugId: $debugId"
        }
    }

    companion object {
        internal const val SENTRY_BUNDLE_ID_OUTPUT = "sentry-bundle-id.properties"
        const val SENTRY_BUNDLE_ID_PROPERTY = "io.sentry.bundle-ids"

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
