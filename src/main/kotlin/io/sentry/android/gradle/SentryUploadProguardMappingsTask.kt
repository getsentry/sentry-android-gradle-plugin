package io.sentry.android.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import java.util.UUID

@Suppress("LeakingThis", "UnstableApiUsage")
@OptIn(ExperimentalStdlibApi::class)
internal abstract class SentryUploadProguardMappingsTask : Exec() {
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

    init {
        description = "Uploads the proguard mappings file"
        group = "upload"
        cliExecutable.finalizeValueOnRead()
        mappingsUuid.finalizeValueOnRead()
        mappingsFile.finalizeValueOnRead()
        sentryProperties.finalizeValueOnRead()
        sentryOrganization.finalizeValueOnRead()
        sentryProject.finalizeValueOnRead()
        autoUpload.finalizeValueOnRead()
    }

    override fun exec() {
        val sentryProperties = sentryProperties.orNull
        if (sentryProperties != null) {
            environment("SENTRY_PROPERTIES", sentryProperties)
        } else {
            logger.info("Sentry properties are not set")
        }
        commandLine(buildList {
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                add("cmd")
                add("/c")
            }

            add(cliExecutable.get())
            add("upload-proguard")
            add("--uuid")
            add(mappingsUuid.get())
            add(mappingsFile.get())
            if (!autoUpload.get()) {
                add("--no-upload")
            }
            val org = sentryOrganization.orNull
            if (org != null) {
                add("--org")
                add(org)
            }
            val project = sentryProject.orNull
            if (project != null) {
                add("--project")
                add(project)
            }
        })
        logger.info("Sentry CLI arguments: $args")
        if (!project.property("sentry.internal.skipUpload").toString().toBoolean()) {
            super.exec()
        }
    }
}