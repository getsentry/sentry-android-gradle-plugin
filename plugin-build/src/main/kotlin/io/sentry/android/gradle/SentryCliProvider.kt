@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryCliValueSource.Params
import io.sentry.android.gradle.SentryPlugin.Companion.logger
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.error
import io.sentry.android.gradle.util.info
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.Properties
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.Input

internal object SentryCliProvider {

    @field:Volatile
    private var memoizedCliPath: String? = null

    /**
     * Return the correct sentry-cli executable path to use for the given project.  This
     * will look for a sentry-cli executable in a local node_modules in case it was put
     * there by sentry-react-native or others before falling back to the global installation.
     */
    @JvmStatic
    @Synchronized
    fun getSentryCliPath(projectDir: File, projectBuildDir: File, rootDir: File): String {
        val cliPath = memoizedCliPath
        if (!cliPath.isNullOrEmpty() && File(cliPath).exists()) {
            logger.info { "Using memoized cli path: $cliPath" }
            return cliPath
        }
        // If a path is provided explicitly use that first.
        logger.info { "Searching cli from sentry.properties file..." }

        searchCliInPropertiesFile(projectDir, rootDir)?.let {
            logger.info { "cli Found: $it" }
            memoizedCliPath = it
            return@getSentryCliPath it
        } ?: logger.info { "sentry-cli not found in sentry.properties file" }

        // next up try a packaged version of sentry-cli
        val cliSuffix = getCliSuffix()
        logger.info { "cliSuffix is $cliSuffix" }

        if (!cliSuffix.isNullOrBlank()) {
            val resourcePath = "/bin/sentry-cli-$cliSuffix"

            // if we are not in a jar, we can use the file directly
            logger.info { "Searching for $resourcePath in resources folder..." }

            searchCliInResources(resourcePath)?.let {
                logger.info { "cli found in resources: $it" }
                memoizedCliPath = it
                return@getSentryCliPath it
            } ?: logger.info { "Failed to load sentry-cli from resource folder" }

            // otherwise we need to unpack into a file
            logger.info { "Trying to load cli from $resourcePath in a temp file..." }

            extractCliFromResources(projectBuildDir, resourcePath)?.let {
                logger.info { "cli extracted from resources into: $it" }
                memoizedCliPath = it
                return@getSentryCliPath it
            } ?: logger.info { "Failed to load sentry-cli from resource folder" }
        }

        logger.error { "Falling back to invoking `sentry-cli` from shell" }
        return "sentry-cli".also { memoizedCliPath = it }
    }

    internal fun getSentryPropertiesPath(projectDir: File, rootDir: File): String? =
        listOf(
            File(projectDir, "sentry.properties"),
            File(rootDir, "sentry.properties")
        ).firstOrNull(File::exists)?.path

    internal fun searchCliInPropertiesFile(projectDir: File, rootDir: File): String? {
        return getSentryPropertiesPath(projectDir, rootDir)?.let { propertiesFile ->
            runCatching {
                Properties()
                    .apply { load(FileInputStream(propertiesFile)) }
                    .getProperty("cli.executable")
            }.getOrNull()
        }
    }

    internal fun searchCliInResources(resourcePath: String): String? {
        val resourceURL = javaClass.getResource(resourcePath)
        val resourceFile = resourceURL?.let { File(it.file) }
        return if (resourceFile?.exists() == true) {
            resourceFile.absolutePath
        } else {
            null
        }
    }

    fun getCliTargetPathForResources(projectBuildDir: File): File {
        // usually <project>/build/tmp/
        return File(File(projectBuildDir, "tmp"), "sentry-cli-${BuildConfig.CliVersion}.exe")
    }

    internal fun extractCliFromResources(projectBuildDir: File, resourcePath: String): String? {
        val resourceStream = javaClass.getResourceAsStream(resourcePath)
        return if (resourceStream != null) {
            val cliFilePath = getCliTargetPathForResources(projectBuildDir)

            val baseFolder = cliFilePath.parentFile
            logger.info { "sentry-cli base folder: ${baseFolder.absolutePath}" }

            if (!baseFolder.exists() && !baseFolder.mkdirs()) {
                logger.error { "sentry-cli base folder could not be created!" }
                return null
            }

            FileOutputStream(cliFilePath).use { output ->
                resourceStream.use { input ->
                    input.copyTo(output)
                }
            }
            cliFilePath.setExecutable(true)
            cliFilePath.deleteOnExit()

            cliFilePath.absolutePath
        } else {
            return null
        }
    }

    internal fun getCliSuffix(): String? {
        // TODO: change to .lowercase(Locale.ROOT) when using Kotlin 1.6
        val osName = System.getProperty("os.name").toLowerCase(Locale.ROOT)
        val osArch = System.getProperty("os.arch")
        return when {
            "mac" in osName -> "Darwin-universal"
            "linux" in osName -> if (osArch == "amd64") "Linux-x86_64" else "Linux-$osArch"
            "win" in osName -> "Windows-i686.exe"
            else -> null
        }
    }
}

abstract class SentryCliValueSource : ValueSource<String, Params> {
    interface Params : ValueSourceParameters {
        @get:Input
        val projectDir: Property<File>

        @get:Input
        val projectBuildDir: Property<File>

        @get:Input
        val rootProjDir: Property<File>
    }

    override fun obtain(): String? {
        return SentryCliProvider.getSentryCliPath(
            parameters.projectDir.get(),
            parameters.projectBuildDir.get(),
            parameters.rootProjDir.get()
        )
    }
}

fun Project.cliExecutableProvider(): Provider<String> {
    return if (GradleVersions.CURRENT >= GradleVersions.VERSION_7_5) {
        // config-cache compatible way to retrieve the cli path, it properly gets invalidated when
        // e.g. switching branches
        providers.of(SentryCliValueSource::class.java) {
            it.parameters.projectDir.set(project.projectDir)
            it.parameters.projectBuildDir.set(project.layout.buildDirectory.asFile.get())
            it.parameters.rootProjDir.set(project.rootDir)
        }
    } else {
        return provider {
            SentryCliProvider.getSentryCliPath(
                project.projectDir,
                project.layout.buildDirectory.asFile.get(),
                project.rootDir
            )
        }
    }
}
