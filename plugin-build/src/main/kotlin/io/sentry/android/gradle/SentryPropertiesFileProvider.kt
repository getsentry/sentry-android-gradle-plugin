package io.sentry.android.gradle

import io.sentry.android.gradle.util.debug
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.gradle.api.Project

internal object SentryPropertiesFileProvider {

  private const val FILENAME = "sentry.properties"

  /**
   * Find sentry.properties and returns the path to the file.
   *
   * @param project the given project
   * @param variant the given variant
   * @return A [String] for the path if sentry.properties is found or null otherwise
   */
  @JvmStatic
  fun getPropertiesFilePath(project: Project, variant: SentryVariant? = null): String? {
    val flavorName = variant?.flavorName.orEmpty()
    val buildTypeName = variant?.buildTypeName.orEmpty()

    val projDir = project.projectDir
    val rootDir = project.rootDir

    // Local Project dirs
    val possibleFiles = mutableListOf<String>()
    if (buildTypeName.isNotBlank()) {
      possibleFiles.add("${projDir}/src/${buildTypeName}/$FILENAME")
    }
    if (flavorName.isNotBlank()) {
      if (buildTypeName.isNotBlank()) {
        possibleFiles.add("${projDir}/src/${buildTypeName}/$flavorName/$FILENAME")
        possibleFiles.add("${projDir}/src/${flavorName}/${buildTypeName}/$FILENAME")
      }
      possibleFiles.add("${projDir}/src/${flavorName}/$FILENAME")
    }
    possibleFiles.add("${projDir}/$FILENAME")

    // Other flavors dirs
    possibleFiles.addAll(
      variant?.productFlavors?.map { "${projDir}/src/${it}/$FILENAME" } ?: emptyList()
    )

    // Root project dirs
    if (buildTypeName.isNotBlank()) {
      possibleFiles.add("${rootDir}/src/${buildTypeName}/$FILENAME")
    }
    if (flavorName.isNotBlank()) {
      possibleFiles.add("${rootDir}/src/${flavorName}/$FILENAME")
      if (buildTypeName.isNotBlank()) {
        possibleFiles.add("${rootDir}/src/${buildTypeName}/${flavorName}/$FILENAME")
        possibleFiles.add("${rootDir}/src/${flavorName}/${buildTypeName}/$FILENAME")
      }
    }
    possibleFiles.add("${rootDir}/$FILENAME")

    return possibleFiles
      .distinct()
      .onEach { project.logger.debug { "Looking for $FILENAME at: $it" } }
      .firstOrNull { File(it).exists() }
      ?.also { project.logger.info { "Found $FILENAME at: $it" } }
  }
}
