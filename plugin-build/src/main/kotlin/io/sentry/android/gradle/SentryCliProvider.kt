package io.sentry.android.gradle

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryPlugin.Companion.logger
import io.sentry.android.gradle.util.error
import io.sentry.android.gradle.util.info
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.Properties

internal object SentryCliProvider {

  /**
   * Return the correct sentry-cli executable path to use for the given project. This will look for
   * a sentry-cli executable in a local node_modules in case it was put there by sentry-react-native
   * or others before falling back to the global installation. In case there's no global
   * installation, and a matching cli is packaged in the resources it will provide a temporary path,
   * without actually extracting it.
   */
  @JvmStatic
  fun getSentryCliPath(projectDir: File, projectBuildDir: File, rootDir: File): String {
    // If a path is provided explicitly use that first.
    logger.info { "Searching cli from sentry.properties file..." }

    searchCliInPropertiesFile(projectDir, rootDir)?.let {
      logger.info { "cli Found: $it" }
      return@getSentryCliPath it
    } ?: logger.info { "sentry-cli not found in sentry.properties file" }

    // next up try a packaged version of sentry-cli
    val cliResLocation = getCliLocationInResources()
    if (!cliResLocation.isNullOrBlank()) {
      logger.info { "cli present in resources: $cliResLocation" }
      return getCliResourcesExtractionPath(projectBuildDir).absolutePath
    }

    logger.error { "Falling back to invoking `sentry-cli` from shell" }
    return "sentry-cli"
  }

  private fun getCliLocationInResources(): String? {
    val cliSuffix = getCliSuffix()
    logger.info { "cliSuffix is $cliSuffix" }

    if (!cliSuffix.isNullOrBlank()) {
      val resourcePath = "/bin/sentry-cli-$cliSuffix"

      // if we are not in a jar, we can use the file directly
      logger.info { "Searching for $resourcePath in resources folder..." }

      getResourceUrl(resourcePath)?.let {
        logger.info { "cli found in resources: $it" }

        // still return the resource path, as it's the one we can use for extraction later
        return resourcePath
      } ?: logger.info { "Failed to load sentry-cli from resource folder" }
    }

    return null
  }

  internal fun getSentryPropertiesPath(projectDir: File, rootDir: File): String? =
    listOf(File(projectDir, "sentry.properties"), File(rootDir, "sentry.properties"))
      .firstOrNull(File::exists)
      ?.path

  internal fun searchCliInPropertiesFile(projectDir: File, rootDir: File): String? {
    return getSentryPropertiesPath(projectDir, rootDir)?.let { propertiesFile ->
      runCatching {
          Properties().apply { load(FileInputStream(propertiesFile)) }.getProperty("cli.executable")
        }
        .getOrNull()
    }
  }

  internal fun getResourceUrl(resourcePath: String): String? =
    javaClass.getResource(resourcePath)?.toString()

  internal fun getCliResourcesExtractionPath(projectBuildDir: File): File {
    // usually <project>/build/tmp/
    return File(projectBuildDir, "tmp/sentry-cli-${BuildConfig.CliVersion}.exe")
  }

  internal fun extractCliFromResources(resourcePath: String, outputPath: File): String? {
    val resourceStream = javaClass.getResourceAsStream(resourcePath)
    return if (resourceStream != null) {
      val baseFolder = outputPath.parentFile
      logger.info { "sentry-cli base folder: ${baseFolder.absolutePath}" }

      if (!baseFolder.exists() && !baseFolder.mkdirs()) {
        logger.error { "sentry-cli base folder could not be created!" }
        return null
      }

      FileOutputStream(outputPath).use { output ->
        resourceStream.use { input -> input.copyTo(output) }
      }
      outputPath.setExecutable(true)
      outputPath.deleteOnExit()

      outputPath.absolutePath
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

  /** Tries to extract the sentry-cli from resources if the computedCliPath does not exist. */
  @Synchronized
  internal fun maybeExtractFromResources(buildDir: File, cliPath: String): String {
    val cli = File(cliPath)
    if (cli.exists()) {
      return cliPath
    }

    val currentExtractionPath = getCliResourcesExtractionPath(buildDir)
    if (currentExtractionPath.exists()) {
      return currentExtractionPath.absolutePath
    }

    // Only auto-extract for paths that look like previous resource extractions
    val buildTmpDir = File(buildDir, "tmp")
    if (cli.absolutePath.startsWith(buildTmpDir.absolutePath)) {
      val cliResPath = getCliLocationInResources()
      if (!cliResPath.isNullOrBlank()) {
        return extractCliFromResources(cliResPath, currentExtractionPath)
          ?: currentExtractionPath.absolutePath
      }
    }

    return cliPath
  }
}
