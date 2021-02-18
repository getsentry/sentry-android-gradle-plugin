package io.sentry.android.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

@Suppress("UnstableApiUsage", "LeakingThis")
abstract class SentryUploadTask : Exec() {
    @get:Input
    abstract val cliExecutable: Property<String>

    @get:InputFile
    @get:Optional
    abstract val sentryPropertiesFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val sentryOrganization: Property<String>

    @get:Input
    @get:Optional
    abstract val sentryProject: Property<String>

    init {
        group = "upload"
        with(cliExecutable) {
            finalizeValueOnRead()
            convention("sentry-cli")
        }
        sentryPropertiesFile.finalizeValueOnRead()
        sentryOrganization.finalizeValueOnRead()
        sentryProject.finalizeValueOnRead()
    }

    @OptIn(ExperimentalStdlibApi::class)
    final override fun exec() {
        val sentryProperties = sentryPropertiesFile.orNull
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
            addArguments()
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

    protected abstract fun MutableList<Any>.addArguments()
}