package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask.Companion.SENTRY_PROGUARD_MAPPING_UUID_PROPERTY
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.PropertiesUtil
import io.sentry.android.gradle.util.ReleaseInfo
import io.sentry.android.gradle.util.asSentryCliExec
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider
import java.io.File

abstract class SentryUploadAppArtifactTask : SentryCliExecTask() {

    @get:InputFile abstract val uuidFile: RegularFileProperty

    @get:InputFiles
    abstract var mappingsFiles: Provider<FileCollection>

    @get:Input
    abstract val autoUploadProguardMapping: Property<Boolean>

    @get:InputFile @get:Optional
    abstract val appArchive: RegularFileProperty

    @get:Input abstract val releaseInfo: Property<ReleaseInfo>


    override fun getArguments(args: MutableList<String>) {
        val uuid = readUuidFromFile(uuidFile.get().asFile)
        val firstExistingFile = mappingsFiles.get().files.firstOrNull { it.exists() }

        val mappingFile =
            if (firstExistingFile == null) {
                logger.warn(
                    "None of the provided mappingFiles was found on disk. " +
                        "Upload is most likely going to be skipped"
                )
                mappingsFiles.get().files.first()
            } else {
                firstExistingFile
            }

        args.add("mobile-app")
        args.add("--app")
        args.add(appArchive.get().asFile.absolutePath)
        args.add("--uuid")
        args.add(uuid)
        args.add(mappingFile.toString())

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
    }

    companion object {
        internal fun readUuidFromFile(file: File): String {
            val props = PropertiesUtil.load(file)
            val uuid: String? = props.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY)
            check(uuid != null) { "$SENTRY_PROGUARD_MAPPING_UUID_PROPERTY property is missing" }
            return uuid
        }

        fun register(
            project: Project,
            extension: SentryPluginExtension,
            sentryTelemetryProvider: Provider<SentryTelemetryService>?,
            debug: Property<Boolean>,
            appArchive: RegularFileProperty,
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
            releaseInfo: ReleaseInfo,
        ): TaskProvider<SentryUploadAppArtifactTask> {
            val uploadMobileAppTask =
                project.tasks.register(
                    "uploadSentryMobileApp$taskSuffix",
                    SentryUploadAppArtifactTask::class.java,
                ) { task ->
                    task.dependsOn(generateUuidTask)
                    task.workingDir(project.rootDir)
                    task.debug.set(debug)
                    task.cliExecutable.set(cliExecutable)
                    task.sentryProperties.set(sentryProperties?.let { file -> project.file(file) })
                    task.uuidFile.set(generateUuidTask.flatMap { it.outputFile })
                    task.mappingsFiles = mappingFiles
                    task.appArchive = appArchive
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
            return uploadMobileAppTask
        }
    }

}
