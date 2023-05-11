package io.sentry.android.gradle.sourcecontext

import org.gradle.api.Project

internal const val ROOT_DIR = "intermediates/sentry"

class OutputPaths(
    private val project: Project,
    variantName: String
) {
    private fun file(path: String) = project.layout.buildDirectory.file(path)
    private fun dir(path: String) = project.layout.buildDirectory.dir(path)

    private val variantDirectory = "$ROOT_DIR/$variantName"

    val bundleIdDir = dir("$variantDirectory/bundle-id")
    val sourceDir = dir("$variantDirectory/source-to-bundle")
    val bundleDir = dir("$variantDirectory/source-bundle")
}
