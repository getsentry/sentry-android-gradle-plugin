package io.sentry.android.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import java.util.UUID

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class SentryUploadProguardMappingsTask : SentryUploadTask() {
    @get:Input
    abstract val mappingsUuid: Property<UUID>

    @get:InputFile
    @get:Optional
    abstract val mappingsFile: RegularFileProperty

    @get:Input
    abstract val autoUpload: Property<Boolean>

    init {
        description = "Uploads the proguard mappings file"
        group = "upload"
        mappingsUuid.finalizeValueOnRead()
        mappingsFile.finalizeValueOnRead()
        with(autoUpload) {
            finalizeValueOnRead()
            convention(true)
        }

        onlyIf { mappingsFile.orNull?.asFile?.exists() == true }
    }

    override fun MutableList<Any>.addArguments() {
        add("upload-proguard")
        add("--uuid")
        add(mappingsUuid.get())
        add(mappingsFile.get())
        if (!autoUpload.get()) {
            add("--no-upload")
        }
    }
}