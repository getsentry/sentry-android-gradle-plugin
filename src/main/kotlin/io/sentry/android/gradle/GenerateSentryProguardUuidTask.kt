package io.sentry.android.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.UUID

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class GenerateSentryProguardUuidTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val outputUuid: Provider<UUID>
        get() = outputUuidInternal

    @get:Internal
    val outputFile: Provider<RegularFile> = outputDirectory.map { it.file("sentry-debug-meta.properties") }

    @get:Internal
    protected abstract val outputUuidInternal: Property<UUID>

    init {
        description = "Generates a unique build ID"
        outputDirectory.finalizeValueOnRead()
        outputUuidInternal.finalizeValueOnRead()
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected fun generateUuid() {
        val uuid = UUID.randomUUID()
        outputUuidInternal.set(uuid)
        outputFile.get().asFile.writeText("io.sentry.ProguardUuids=$uuid")
    }
}