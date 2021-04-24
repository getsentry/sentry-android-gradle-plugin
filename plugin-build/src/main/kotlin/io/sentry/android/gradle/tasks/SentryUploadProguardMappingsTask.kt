package io.sentry.android.gradle.tasks

import java.util.UUID
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

abstract class SentryUploadProguardMappingsTask : Exec() {

    init {
        description = "Uploads the proguard mappings file to Sentry"
    }

    @get:Input
    abstract val cliExecutable: Property<String>

    @get:Input
    abstract val mappingsUuid: Property<UUID>

    @get:InputFile
    abstract val mappingsFile: RegularFileProperty

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
    abstract val autoUpload: Property<Boolean>

    override fun exec() {
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
        val args = mutableListOf(
            cliExecutable.get(),
            "upload-proguard",
            "--uuid",
            mappingsUuid.get().toString(),
            mappingsFile.get().toString()
        )

        if (!autoUpload.get()) {
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
}
