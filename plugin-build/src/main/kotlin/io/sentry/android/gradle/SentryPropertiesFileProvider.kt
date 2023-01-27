package io.sentry.android.gradle

import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.AndroidVariant
import java.io.File
import org.gradle.api.Project

internal object SentryPropertiesFileProvider {

    private const val FILENAME = "sentry.properties"

    /**
     * Find sentry.properties and returns the path to the file.
     * @param project the given project
     * @param variant the given variant
     * @return A [String] for the path if sentry.properties is found or null otherwise
     */
    @JvmStatic
    fun getPropertiesFilePath(project: Project, variant: AndroidVariant): String? {
        val flavorName = variant.flavorName.orEmpty()
        val buildTypeName = variant.buildTypeName.orEmpty()

        val projDir = project.projectDir
        val rootDir = project.rootDir

        val sep = File.separator

        // Local Project dirs
        val possibleFiles = mutableListOf(
            "${projDir}${sep}src${sep}${buildTypeName}${sep}$FILENAME"
        )
        if (flavorName.isNotBlank()) {
            possibleFiles.add(
                "${projDir}${sep}src${sep}${buildTypeName}${sep}$flavorName${sep}$FILENAME"
            )
            possibleFiles.add(
                "${projDir}${sep}src${sep}${flavorName}${sep}${buildTypeName}${sep}$FILENAME"
            )
            possibleFiles.add("${projDir}${sep}src${sep}${flavorName}${sep}$FILENAME")
        }
        possibleFiles.add("${projDir}${sep}$FILENAME")

        // Other flavors dirs
        possibleFiles.addAll(
            variant.productFlavors.map { "${projDir}${sep}src${sep}${it}${sep}$FILENAME" }
        )

        // Root project dirs
        possibleFiles.add("${rootDir}${sep}src${sep}${buildTypeName}${sep}$FILENAME")
        if (flavorName.isNotBlank()) {
            possibleFiles.add("${rootDir}${sep}src${sep}${flavorName}${sep}$FILENAME")
            possibleFiles.add(
                "${rootDir}${sep}src${sep}${buildTypeName}${sep}${flavorName}${sep}$FILENAME"
            )
            possibleFiles.add(
                "${rootDir}${sep}src${sep}${flavorName}${sep}${buildTypeName}${sep}$FILENAME"
            )
        }
        possibleFiles.add("${rootDir}${sep}$FILENAME")

        return possibleFiles.distinct().asSequence()
            .onEach { project.logger.info { "Looking for $FILENAME at: $it" } }
            .firstOrNull { File(it).exists() }
            ?.also { project.logger.info { "Found $FILENAME at: $it" } }
    }
}
