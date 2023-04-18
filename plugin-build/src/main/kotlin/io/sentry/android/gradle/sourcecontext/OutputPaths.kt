package io.sentry.android.gradle.sourcecontext

import org.gradle.api.Project

internal const val ROOT_DIR = "reports/sentry-source-bundle"

/*internal*/ class OutputPaths(
    private val project: Project,
    variantName: String
) {

    private fun file(path: String) = project.layout.buildDirectory.file(path)
    private fun dir(path: String) = project.layout.buildDirectory.dir(path)

    private val variantDirectory = "$ROOT_DIR/$variantName"
    private val intermediatesDir = "${variantDirectory}/intermediates"
    private val sentryIntermediatesDir = "${intermediatesDir}/sentry"

    val bundleIdDir = dir("${sentryIntermediatesDir}/bundle-id")
    val sourceDir = dir("${sentryIntermediatesDir}/source-to-bundle")
    val bundleDir = dir("${sentryIntermediatesDir}/source-bundle")
}
