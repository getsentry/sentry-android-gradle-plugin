package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project
import java.io.File

internal object SentryPropertiesFileProvider {

    private const val FILENAME = "sentry.properties"

    /**
     * Find sentry.properties and returns the path to the file.
     * @param project the given project
     * @param variant the given variant
     * @return A [String] for the path if sentry.properties is found or null otherwise
     */
    @JvmStatic
    fun getPropertiesFilePath(project: Project, variant: ApplicationVariant): String? {
        val flavorName = variant.flavorName
        val buildTypeName = variant.buildType.name

        val projDir = project.projectDir
        val rootDir = project.rootDir

        // Local Project dirs
        val possibleFiles = mutableListOf(
            "$projDir/src/$buildTypeName/$FILENAME"
        )
        if (flavorName.isNotBlank()) {
            possibleFiles.add("$projDir/src/$buildTypeName/$flavorName/$FILENAME")
            possibleFiles.add("$projDir/src/$flavorName/$buildTypeName/$FILENAME")
            possibleFiles.add("$projDir/src/$flavorName/$FILENAME")
        }
        possibleFiles.add("$projDir/$FILENAME")

        // Other flavors dirs
        possibleFiles.addAll(variant.productFlavors.map { "$projDir/src/${it.name}/$FILENAME" })

        // Root project dirs
        possibleFiles.add("$rootDir/src/$buildTypeName/$FILENAME")
        if (flavorName.isNotBlank()) {
            possibleFiles.add("$rootDir/src/$flavorName/$FILENAME")
            possibleFiles.add("$rootDir/src/$buildTypeName/$flavorName/$FILENAME")
            possibleFiles.add("$rootDir/src/$flavorName/$buildTypeName/$FILENAME")
        }
        possibleFiles.add("$rootDir/$FILENAME")

        return possibleFiles.distinct().asSequence()
            .onEach { project.logger.info("[sentry] Looking for $FILENAME at: $it") }
            .firstOrNull { File(it).exists() }
            ?.also { project.logger.info("[sentry] Found $FILENAME at: $it") }
    }
}
