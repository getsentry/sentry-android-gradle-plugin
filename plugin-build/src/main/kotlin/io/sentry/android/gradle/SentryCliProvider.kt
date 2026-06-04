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
import java.util.concurrent.TimeUnit
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

internal object SentryCliProvider {

  /**
   * Return the correct sentry-cli executable path to use for the given project. This will look for
   * a sentry-cli executable in a local node_modules in case it was put there by sentry-react-native
   * or others before falling back to the global installation. In case there's no global
   * installation, and a matching cli is packaged in the resources it will provide a temporary path,
   * without actually extracting it.
   */
  @JvmStatic
  fun getSentryCliPath(
    projectDir: DirectoryProperty,
    projectBuildDir: DirectoryProperty,
    rootDir: DirectoryProperty,
  ): String {
    logger.info { "Searching cli from sentry.properties file..." }

    searchCliInPropertiesFile(projectDir, rootDir)?.let {
      logger.info { "cli Found: $it" }
      return@getSentryCliPath it
    } ?: logger.info { "sentry-cli not found in sentry.properties file" }

    val cliResLocation = getCliLocationInResources()
    if (!cliResLocation.isNullOrBlank()) {
      logger.info { "cli present in resources: $cliResLocation" }
      return getCliResourcesExtractionPath(projectBuildDir).get().asFile.absolutePath
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

  internal fun getSentryPropertiesPath(
    projectDir: DirectoryProperty,
    rootDir: DirectoryProperty,
  ): String? =
    listOf(projectDir.file("sentry.properties"), rootDir.file("sentry.properties"))
      .map { it.get().asFile }
      .firstOrNull(File::exists)
      ?.path

  internal fun searchCliInPropertiesFile(
    projectDir: DirectoryProperty,
    rootDir: DirectoryProperty,
  ): String? {
    return getSentryPropertiesPath(projectDir, rootDir)?.let { propertiesFile ->
      runCatching {
          Properties().apply { load(FileInputStream(propertiesFile)) }.getProperty("cli.executable")
        }
        .getOrNull()
    }
  }

  internal fun getResourceUrl(resourcePath: String): String? =
    javaClass.getResource(resourcePath)?.toString()

  internal fun getCliResourcesExtractionPath(
    projectBuildDir: DirectoryProperty
  ): Provider<RegularFile> {
    // usually <project>/build/tmp/
    return projectBuildDir.dir("tmp").map { it.file("sentry-cli-${BuildConfig.CliVersion}.exe") }
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
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
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
  internal fun maybeExtractFromResources(buildDir: DirectoryProperty, cliPath: String): String {
    val cli = File(cliPath)
    if (!cli.exists()) {
      // we only want to auto-extract if the path matches the pre-computed one
      if (
        File(cliPath)
          .absolutePath
          .equals(getCliResourcesExtractionPath(buildDir).get().asFile.absolutePath)
      ) {
        val cliResPath = getCliLocationInResources()
        if (!cliResPath.isNullOrBlank()) {
          return extractCliFromResources(cliResPath, cli) ?: cliPath
        }
      }
    }
    return cliPath
  }
}

abstract class SentryCliValueSource : ValueSource<String, Params> {
  interface Params : ValueSourceParameters {
    @get:Input val projectDir: DirectoryProperty

    @get:Input val projectBuildDir: DirectoryProperty

    @get:Input val rootProjDir: DirectoryProperty
  }

  override fun obtain(): String? {
    return SentryCliProvider.getSentryCliPath(
      parameters.projectDir,
      parameters.projectBuildDir,
      parameters.rootProjDir,
    )
  }
}

fun Project.cliExecutableProvider(): Provider<String> {
  // config-cache compatible way to retrieve the cli path, it properly gets invalidated when
  // e.g. switching branches
  return providers.of(SentryCliValueSource::class.java) {
    it.parameters.projectDir.set(layout.projectDirectory)
    it.parameters.projectBuildDir.set(layout.buildDirectory)
    it.parameters.rootProjDir.set(getIsolatedRootProjectDir())
  }
}

private fun Project.getIsolatedRootProjectDir(): Directory {
  return if (GradleVersions.CURRENT >= GradleVersions.VERSION_8_8) {
    isolated.rootProject.projectDirectory
  } else {
    rootProject.layout.projectDirectory
  }
}

/**
 * Resolves the default Sentry organization by running `sentry-cli info`. Implemented as a
 * ValueSource so the external process is started in a configuration-cache compatible way; querying
 * the returned provider during task execution keeps the process off the configuration phase.
 */
abstract class SentryOrgValueSource : ValueSource<String, SentryOrgValueSource.Params> {
  interface Params : ValueSourceParameters {
    @get:Input val projectDir: DirectoryProperty

    @get:Input val projectBuildDir: DirectoryProperty

    @get:Input val rootProjDir: DirectoryProperty

    @get:Input @get:Optional val url: Property<String>

    @get:Input @get:Optional val authToken: Property<String>

    @get:Input @get:Optional val propertiesFilePath: Property<String>
  }

  override fun obtain(): String? {
    return try {
      val cliPath =
        SentryCliProvider.getSentryCliPath(
          parameters.projectDir,
          parameters.projectBuildDir,
          parameters.rootProjDir,
        )
      val resolvedCli =
        SentryCliProvider.maybeExtractFromResources(parameters.projectBuildDir, cliPath)

      val args = mutableListOf(resolvedCli)
      parameters.url.orNull?.let {
        args.add("--url")
        args.add(it)
      }
      args.add("--log-level=error")
      args.add("info")

      val processBuilder = ProcessBuilder(args).redirectErrorStream(true)
      parameters.propertiesFilePath.orNull?.let {
        processBuilder.environment()["SENTRY_PROPERTIES"] = it
      }
      parameters.authToken.orNull?.let { processBuilder.environment()["SENTRY_AUTH_TOKEN"] = it }
      processBuilder.environment()["SENTRY_PIPELINE"] =
        "sentry-gradle-plugin/${BuildConfig.Version}"

      val process = processBuilder.start()
      // Wait with the timeout before reading the output: reading first would block indefinitely if
      // the process hangs (e.g. a network stall), bypassing the timeout. `info` output is small and
      // bounded, so it cannot fill the pipe buffer and stall the process before we read it here.
      if (!process.waitFor(CLI_INFO_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return null
      }
      if (process.exitValue() != 0) {
        return null
      }
      val output = process.inputStream.bufferedReader().readText()
      ORG_REGEX.find(output)?.groupValues?.getOrNull(1)?.takeUnless { it == "-" }
    } catch (t: Throwable) {
      logger.info { "Failed to fetch default org from sentry-cli: ${t.message}" }
      null
    }
  }

  companion object {
    private val ORG_REGEX = Regex("""(?m)Default Organization: (.*)$""")
    private const val CLI_INFO_TIMEOUT_SECONDS = 5L
  }
}

fun Project.defaultOrgProvider(
  url: String?,
  authToken: String?,
  propertiesFilePath: String?,
): Provider<String> {
  return providers.of(SentryOrgValueSource::class.java) {
    it.parameters.projectDir.set(layout.projectDirectory)
    it.parameters.projectBuildDir.set(layout.buildDirectory)
    it.parameters.rootProjDir.set(getIsolatedRootProjectDir())
    url?.let { value -> it.parameters.url.set(value) }
    authToken?.let { value -> it.parameters.authToken.set(value) }
    propertiesFilePath?.let { value -> it.parameters.propertiesFilePath.set(value) }
  }
}
