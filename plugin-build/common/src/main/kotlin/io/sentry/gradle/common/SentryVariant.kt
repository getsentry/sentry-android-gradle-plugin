package io.sentry.gradle.common

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Common interface to be used from the main source set to access Android/JVM variants.
 * Its implementations live within the compatibility source sets.
 */
interface SentryVariant {
    val name: String
    val flavorName: String?
    val buildTypeName: String?
    val productFlavors: List<String>
    val isMinifyEnabled: Boolean
    val isDebuggable: Boolean
    val packageProvider: TaskProvider<out Task>? get() = null
    val assembleProvider: TaskProvider<out Task>? get() = null
    val installProvider: TaskProvider<out Task>? get() = null
    fun mappingFileProvider(project: Project): Provider<FileCollection>
    fun sources(
        project: Project,
        additionalSources: Provider<out Collection<Directory>>
    ): Provider<out Collection<Directory>>
}
