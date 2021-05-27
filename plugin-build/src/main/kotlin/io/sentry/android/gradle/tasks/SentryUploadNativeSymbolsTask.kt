package io.sentry.android.gradle.tasks

import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class SentryUploadNativeSymbolsTask : Exec() {

    init {
        description = "Uploads native symbols to Sentry"
    }

    @get:Input
    abstract val cliExecutable: Property<String>

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
    abstract val includeNativeSources: Property<Boolean>

    @get:Internal
    abstract val variantName: Property<String>

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
            logger.info("[sentry] sentryProperties is null")
        }
    }

    internal fun computeCommandLineArgs(): List<String> {
        val args = mutableListOf(
            cliExecutable.get(),
            "upload-dif"
        )

        sentryOrganization.orNull?.let {
            args.add("--org")
            args.add(it)
        }

        sentryProject.orNull?.let {
            args.add("--project")
            args.add(it)
        }

        val sep = File.separator

        // eg absoluteProjectFolderPath/build/intermediates/merged_native_libs/{variantName}
        // where {variantName} could be debug/release...
        args.add(
            "${project.projectDir}${sep}build${sep}intermediates" +
                "${sep}merged_native_libs${sep}${variantName.get()}"
        )

        // Only include sources if includeNativeSources is enabled, as this is opt-in feature
        if (includeNativeSources.get()) {
            args.add("--include-sources")
        }

        if (Os.isFamily(FAMILY_WINDOWS)) {
            args.add(0, "cmd")
            args.add(1, "/c")
        }
        return args
    }
}
