package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.SentryPropertiesFileProvider
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.tasks.SentryCliExec
import io.sentry.android.gradle.util.PropertiesUtil
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider

abstract class BundleSourcesTask : SentryCliExec() {

    init {
        group = SENTRY_GROUP
        description = "Creates a Sentry source bundle file."

        @Suppress("LeakingThis")
        onlyIf {
            includeSourceContext.getOrElse(false) &&
                !sourceDir.asFileTree.isEmpty
        }
    }

    @get:Input
    abstract val includeSourceContext: Property<Boolean>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val debug: Property<Boolean>

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:InputFile
    abstract val bundleIdFile: RegularFileProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val sentryOrganization: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryProject: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryAuthToken: Property<String>

    override fun exec() {
        computeCommandLineArgs().let {
            commandLine(it)
            logger.info { "cli args: $it" }
        }
        setSentryPropertiesEnv()
        setSentryAuthTokenEnv()
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

    internal fun setSentryAuthTokenEnv() {
        val sentryAuthToken = sentryAuthToken.orNull
        if (sentryAuthToken != null) {
            environment("SENTRY_AUTH_TOKEN", sentryAuthToken)
        } else {
            logger.info { "sentryAuthToken is null" }
        }
    }

    internal fun computeCommandLineArgs(): List<String> {
        val bundleId = readBundleIdFromFile(bundleIdFile.get().asFile)
        val args = mutableListOf(cliExecutable.get())

        if (debug.getOrElse(false)) {
            args.add("--log-level=debug")
        }

        args.add("debug-files")
        args.add("bundle-jvm")
        args.add("--output=${output.asFile.get().absolutePath}")
        args.add("--debug-id=$bundleId")

        sentryOrganization.orNull?.let {
            args.add("--org")
            args.add(it)
        }

        sentryProject.orNull?.let {
            args.add("--project")
            args.add(it)
        }

        args.add(sourceDir.get().asFile.absolutePath)

        return args
    }

    companion object {
        internal fun readBundleIdFromFile(file: File): String {
            val props = PropertiesUtil.load(file)
            val bundleId: String? = props.getProperty(SENTRY_BUNDLE_ID_PROPERTY)
            check(bundleId != null) {
                "$SENTRY_BUNDLE_ID_PROPERTY property is missing"
            }
            return bundleId
        }

        fun register(
            project: Project,
            variant: SentryVariant,
            generateDebugIdTask: TaskProvider<GenerateBundleIdTask>,
            collectSourcesTask: TaskProvider<CollectSourcesTask>,
            output: Provider<Directory>,
            debug: Property<Boolean>,
            cliExecutable: String,
            sentryOrg: Provider<String>,
            sentryProject: Provider<String>,
            sentryAuthToken: Property<String>,
            includeSourceContext: Property<Boolean>,
            taskSuffix: String = ""
        ): TaskProvider<BundleSourcesTask> {
            return project.tasks.register(
                "sentryBundleSources$taskSuffix",
                BundleSourcesTask::class.java
            ) { task ->
                task.debug.set(debug)
                task.sentryOrganization.set(sentryOrg)
                task.sentryProject.set(sentryProject)
                task.sentryAuthToken.set(sentryAuthToken)
                task.sourceDir.set(collectSourcesTask.flatMap { it.output })
                task.cliExecutable.set(cliExecutable)
                SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)?.let {
                    task.sentryProperties.set(File(it))
                }
                task.bundleIdFile.set(generateDebugIdTask.flatMap { it.outputFile })
                task.output.set(output)
                task.includeSourceContext.set(includeSourceContext)
            }
        }
    }
}
