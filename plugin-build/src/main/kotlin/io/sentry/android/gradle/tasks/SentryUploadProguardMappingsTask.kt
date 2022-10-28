package io.sentry.android.gradle.tasks

import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class SentryUploadProguardMappingsTask : Exec() {

    init {
        description = "Uploads the proguard mappings file to Sentry"
    }

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

    override fun exec() {
        if (!mappingsFiles.isPresent || mappingsFiles.get().isEmpty) {
            error("[sentry] Mapping files are missing!")
        }
        computeCommandLineArgs().let {
            commandLine(it)
            logger.info("cli args: $it")
        }
        setSentryPropertiesEnv()
        super.exec()
    }

    internal fun setSentryPropertiesEnv() {
        val sentryProperties = sentryProperties.orNull
        if (sentryProperties != null) {
            environment("SENTRY_PROPERTIES", sentryProperties)
        } else {
            logger.info("propsFile is null")
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

        val args = mutableListOf(
            cliExecutable.get(),
            "upload-proguard",
            "--uuid",
            uuid,
            mappingFile.toString()
        )

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

        if (Os.isFamily(FAMILY_WINDOWS)) {
            args.add(0, "cmd")
            args.add(1, "/c")
        }
        return args
    }

    companion object {
        private const val PROPERTY_PREFIX = "io.sentry.ProguardUuids="

        internal fun readUuidFromFile(file: File): String {
            check(file.exists()) {
                "UUID properties file is missing"
            }
            val content = file.readText().trim()
            check(content.startsWith(PROPERTY_PREFIX)) {
                "io.sentry.ProguardUuids property is missing"
            }
            return content.removePrefix(PROPERTY_PREFIX)
        }
    }
}
