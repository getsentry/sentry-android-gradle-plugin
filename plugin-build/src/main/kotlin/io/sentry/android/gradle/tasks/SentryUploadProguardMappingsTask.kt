package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask.Companion.SENTRY_PROGUARD_MAPPING_UUID_PROPERTY
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.PropertiesUtil
import io.sentry.android.gradle.util.ReleaseInfo
import io.sentry.android.gradle.util.asSentryCliExec
import io.sentry.android.gradle.util.info
import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider

abstract class SentryUploadProguardMappingsTask : Exec() {

    init {
        description = "Uploads the proguard mappings file to Sentry"
    }

    @get:Input
    @get:Optional
    abstract val debug: Property<Boolean>

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:InputFile
    abstract val uuidFile: RegularFileProperty

    @get:InputFiles
    abstract var mappingsFiles: Provider<FileCollection>

    @get:InputFile
    @get:Optional
    abstract val sentryProperties: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val sentryOrganization: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryProject: Property<String>

    @get:Input
    abstract val autoUploadProguardMapping: Property<Boolean>

    @get:Input
    abstract val releaseInfo: Property<ReleaseInfo>

    @get:Input
    @get:Optional
    abstract val sentryAuthToken: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryUrl: Property<String>

    @get:Internal
    abstract val sentryTelemetryService: Property<SentryTelemetryService>

    override fun exec() {
        if (!mappingsFiles.isPresent || mappingsFiles.get().isEmpty) {
            error("[sentry] Mapping files are missing!")
        }
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
            logger.info { "propsFile is null" }
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
        val uuid = readUuidFromFile(uuidFile.get().asFile)
        val firstExistingFile = mappingsFiles.get().files.firstOrNull { it.exists() }

        val mappingFile = if (firstExistingFile == null) {
            logger.warn(
                "None of the provided mappingFiles was found on disk. " +
                    "Upload is most likely going to be skipped"
            )
            mappingsFiles.get().files.first()
        } else {
            firstExistingFile
        }

        val args = mutableListOf(cliExecutable.get())

        if (debug.getOrElse(false)) {
            args.add("--log-level=debug")
        }

        sentryTelemetryService.orNull?.traceCli()?.let { args.addAll(it) }

        sentryUrl.orNull?.let {
            args.add("--url")
            args.add(it)
        }

        args.add("upload-proguard")
        args.add("--uuid")
        args.add(uuid)
        args.add(mappingFile.toString())

        if (!autoUploadProguardMapping.get()) {
            args.add("--no-upload")
        }

        sentryOrganization.orNull?.let {
            args.add("--org")
            args.add(it)
        }

        sentryProject.orNull?.let {
            args.add("--project")
            args.add(it)
        }

        releaseInfo.get().let {
            it.versionCode?.let { versionCode ->
                args.add("--version-code")
                args.add(versionCode.toString())
            }
            args.add("--app-id")
            args.add(it.applicationId)
            args.add("--version")
            args.add(it.versionName)
        }

        if (Os.isFamily(FAMILY_WINDOWS)) {
            args.add(0, "cmd")
            args.add(1, "/c")
        }
        return args
    }

    companion object {
        internal fun readUuidFromFile(file: File): String {
            val props = PropertiesUtil.load(file)
            val uuid: String? = props.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY)
            check(uuid != null) {
                "$SENTRY_PROGUARD_MAPPING_UUID_PROPERTY property is missing"
            }
            return uuid
        }

        fun register(
            project: Project,
            extension: SentryPluginExtension,
            sentryTelemetryProvider: Provider<SentryTelemetryService>?,
            debug: Property<Boolean>,
            cliExecutable: Provider<String>,
            sentryProperties: String?,
            generateUuidTask: Provider<SentryGenerateProguardUuidTask>,
            mappingFiles: Provider<FileCollection>,
            sentryOrg: Provider<String>,
            sentryProject: Provider<String>,
            sentryAuthToken: Property<String>,
            sentryUrl: Property<String>,
            autoUploadProguardMapping: Property<Boolean>,
            taskSuffix: String = "",
            releaseInfo: ReleaseInfo
        ): TaskProvider<SentryUploadProguardMappingsTask> {
            val uploadSentryProguardMappingsTask = project.tasks.register(
                "uploadSentryProguardMappings$taskSuffix",
                SentryUploadProguardMappingsTask::class.java
            ) { task ->
                task.dependsOn(generateUuidTask)
                task.workingDir(project.rootDir)
                task.debug.set(debug)
                task.cliExecutable.set(cliExecutable)
                task.sentryProperties.set(
                    sentryProperties?.let { file -> project.file(file) }
                )
                task.uuidFile.set(generateUuidTask.flatMap { it.outputFile })
                task.mappingsFiles = mappingFiles
                task.autoUploadProguardMapping.set(autoUploadProguardMapping)
                task.sentryOrganization.set(sentryOrg)
                task.sentryProject.set(sentryProject)
                task.releaseInfo.set(releaseInfo)
                task.sentryAuthToken.set(sentryAuthToken)
                task.sentryUrl.set(sentryUrl)
                sentryTelemetryProvider?.let { task.sentryTelemetryService.set(it) }
                task.asSentryCliExec()
                task.withSentryTelemetry(extension, sentryTelemetryProvider)
            }
            return uploadSentryProguardMappingsTask
        }
    }
}
