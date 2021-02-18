package io.sentry.android.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional

@Suppress("LeakingThis", "UnstableApiUsage")
internal abstract class SentryUploadNativeSymbolsTask : SentryUploadTask() {
    @get:InputDirectory
    @get:Optional
    abstract val symbolsDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val includeNativeSources: Property<Boolean>

    init {
        description = "Uploads Native symbols"
        group = "upload"
        symbolsDirectory.finalizeValueOnRead()
        with(includeNativeSources) {
            finalizeValueOnRead()
            convention(false)
        }

        onlyIf { symbolsDirectory.orNull?.asFile?.exists() == true }
    }

    override fun MutableList<Any>.addArguments() {
        add("upload-dif")
        val symbolsDirectory = symbolsDirectory.get().asFile
        logger.info("symbols directory: $symbolsDirectory")
        add(symbolsDirectory)

        if (includeNativeSources.get()) {
            add("--include-sources")
        }
    }
}