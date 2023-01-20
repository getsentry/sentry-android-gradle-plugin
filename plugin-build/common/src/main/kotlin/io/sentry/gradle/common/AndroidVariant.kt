package io.sentry.gradle.common

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

interface AndroidVariant {
    val name: String
    val flavorName: String?
    val buildTypeName: String?
    val productFlavors: List<String>
    val isMinifyEnabled: Boolean
    fun mappingFileProvider(project: Project): Provider<FileCollection>
}
