@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import io.sentry.android.gradle.SentryCliValueSource.Params
import io.sentry.android.gradle.SentryPlugin.Companion.logger
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.error
import io.sentry.android.gradle.util.info
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
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
    fun getSentryCliPath(projectDir: File, rootDir: File): String {
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

            val tmpDirPrefix = try {
                // only specific for some tests for deterministic behavior when executing in parallel
                System.getProperty("sentryCliTempFolder")
            } catch (e: Throwable) {
                null
            }

            if (tmpDirPrefix.isNullOrEmpty()) {
                // if we are not in a jar, we can use the file directly
                logger.info { "Searching for $resourcePath in resources folder..." }

                searchCliInResources(resourcePath)?.let {
                    logger.info { "cli found in resources: $it" }
                    memoizedCliPath = it
                    return@getSentryCliPath it
                } ?: logger.info { "Failed to load sentry-cli from resource folder" }
            }

            // otherwise we need to unpack into a file
            logger.info { "Trying to load cli from $resourcePath in a temp file..." }

            loadCliFromResourcesToTemp(resourcePath, tmpDirPrefix)?.let {
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

    internal fun loadCliFromResourcesToTemp(
        resourcePath: String,
        tmpDirPrefix: String? = null
    ): String? {
        val resourceStream = javaClass.getResourceAsStream(resourcePath)
        val tempFile = File.createTempFile(
            ".sentry-cli",
            ".exe",
            tmpDirPrefix?.let { Files.createTempDirectory(it).toFile() }
        ).apply {
            deleteOnExit()
            setExecutable(true)
        }

        return if (resourceStream != null) {
            FileOutputStream(tempFile).use { output ->
                resourceStream.use { input ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } else {
            null
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
        val rootProjDir: Property<File>
    }

    override fun obtain(): String? {
        return SentryCliProvider.getSentryCliPath(
            parameters.projectDir.get(),
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
            it.parameters.rootProjDir.set(project.rootDir)
        }
    } else {
        return provider {
            SentryCliProvider.getSentryCliPath(
                project.projectDir,
                project.rootDir
            )
        }
    }
}
