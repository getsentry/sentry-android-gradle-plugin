package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SentryPluginUtils
import io.sentry.android.gradle.util.artifactsFor
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class SentryExternalDependenciesReportTask : DefaultTask() {

    @get:Input
    abstract val includeReport: Property<Boolean>

    init {
        description = "Generates an external dependencies report"

        if (GradleVersions.CURRENT >= GradleVersions.VERSION_7_4) {
            @Suppress("LeakingThis")
            notCompatibleWithConfigurationCache("Cannot serialize Configurations")
        }
        @Suppress("LeakingThis")
        onlyIf { includeReport.get() }
    }

    @Transient
    private lateinit var runtimeConfiguration: Configuration

    fun setRuntimeConfiguration(configuration: Configuration) {
        runtimeConfiguration = configuration
    }

    @get:Input
    abstract val attributeValueJar: Property<String>

    // this is a proper input, so our task gets triggered whenever the dependency set changes
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFiles
    fun getRuntimeClasspath(): FileCollection = runtimeConfiguration.artifactsFor(
        attributeValueJar.get()
    )

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun action() {
        val outputFile = SentryPluginUtils.getAndDeleteFile(output)
        outputFile.parentFile.mkdirs()

        val dependencies = runtimeConfiguration
            .incoming
            .resolutionResult
            .allComponents
            // we're only interested in external deps
            .filter { it.id is ModuleComponentIdentifier }
            // and those that have proper version defined (e.g. flat jars don't have it)
            .filter { it.moduleVersion?.version?.isNotEmpty() == true }
            .map { it.id.displayName }
            .toSortedSet()

        outputFile.writeText(dependencies.joinToString("\n"))
    }
}
