package io.sentry.android.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.util.Locale

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class UnpackSentryCliTask : DefaultTask() {
    @get:Input
    abstract val resourcePath: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        resourcePath.finalizeValueOnRead()
        outputFile.finalizeValueOnRead()
    }

    @TaskAction
    protected fun unpackFile() {
        UnpackSentryCliTask::class.java.getResourceAsStream(resourcePath.get()).use { inputStream ->
            outputFile.get().asFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        outputFile.get().asFile.setExecutable(true)
    }

    companion object {
        internal fun Project.tryRegisterUnpackCliTask(): TaskProvider<UnpackSentryCliTask>? {
            val osName = System.getProperty("os.name").toLowerCase(Locale.ROOT)
            project.logger.info("osName: $osName")
            val cliSuffix = when {
                "mac" in osName -> "Darwin-x86_64"
                "linux" in osName -> {
                    val arch = System.getProperty("os.arch")
                        .takeUnless { it == "amd64" }
                        ?: "x86_64"
                    "Linux-$arch"
                }
                "win" in osName -> "Windows-i686.exe"
                else -> {
                    project.logger.info("Unknown OS $osName")
                    return null
                }
            }


            return tasks.register("unpackSentryCli", UnpackSentryCliTask::class.java) { task ->
                task.resourcePath.set("/bin/sentry-cli-$cliSuffix")
                val ext = if (Os.isFamily(Os.FAMILY_WINDOWS)) ".exe" else ""
                task.outputFile.set(layout.buildDirectory.file("sentry/cli/sentry-cli$ext"))
            }
        }
    }
}