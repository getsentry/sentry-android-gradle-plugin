package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

abstract class BundleSourcesTask : Exec() {

    init {
        group = SENTRY_GROUP
        description = "Creates a Sentry source bundle file."
    }

    /** The Groovy source of the current project. */
//    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:InputFile
    abstract val bundleIdFile: RegularFileProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    override fun exec() {
        computeCommandLineArgs().let {
            commandLine(it)
            logger.error("cli args: $it")
        }
        setSentryPropertiesEnv()
        super.exec()
    }

    internal fun setSentryPropertiesEnv() {
        val sentryProperties = sentryProperties.orNull
        if (sentryProperties != null) {
            environment("SENTRY_PROPERTIES", sentryProperties)
        } else {
            logger.info { "sentryProperties is null" }
        }
    }

    internal fun computeCommandLineArgs(): List<String> {
        val bundleId = readBundleIdFromFile(bundleIdFile.get().asFile)
        val args = mutableListOf(
            cliExecutable.get(),
            "--log-level=debug",
            "debug-files",
            "bundle-jvm"
        )
        args.add("--output=${output.asFile.get().absolutePath}")
        args.add("--debug-id=${bundleId}")
        args.add(sourceDir.get().asFile.absolutePath)
        return args
    }

    companion object {
        private const val PROPERTY_PREFIX = "${SENTRY_BUNDLE_ID_PROPERTY}="

        internal fun readBundleIdFromFile(file: File): String {
            check(file.exists()) {
                "Bundle ID properties file is missing"
            }
            val content = file.readText().trim()
            check(content.startsWith(PROPERTY_PREFIX)) {
                "${SENTRY_BUNDLE_ID_PROPERTY} property is missing"
            }
            return content.removePrefix(PROPERTY_PREFIX)
        }

        fun register(
            project: Project,
            variant: AndroidVariant,
            generateDebugIdTask: TaskProvider<GenerateBundleIdTask>,
            collectSourcesTask: TaskProvider<CollectSourcesTask>,
            input: Provider<Directory>,
            output: Provider<Directory>,
            cliExecutable: String,
            taskSuffix: String = ""
        ): TaskProvider<BundleSourcesTask> {
            return project.tasks.register(
                "sentryBundleSources${taskSuffix}",
                BundleSourcesTask::class.java
            ) { task ->
                task.dependsOn(generateDebugIdTask)
                task.dependsOn(collectSourcesTask)

                task.sourceDir.set(input)
                task.cliExecutable.set(cliExecutable)
                SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)?.let {
                    task.sentryProperties.set(File(it))
                }
                task.bundleIdFile.set(generateDebugIdTask.flatMap { it.outputFile })
                task.output.set(output)
            }
        }
    }
}
