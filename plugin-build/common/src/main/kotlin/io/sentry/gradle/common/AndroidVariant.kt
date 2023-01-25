package io.sentry.gradle.common

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

interface AndroidVariant {
    val name: String
    val flavorName: String?
    val buildTypeName: String?
    val productFlavors: List<String>
    val isMinifyEnabled: Boolean
    val packageProvider: TaskProvider<out Task>? get() = null
    val assembleProvider: TaskProvider<out Task>? get() = null
    fun mappingFileProvider(project: Project): Provider<FileCollection>
}
