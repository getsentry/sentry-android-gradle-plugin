package io.sentry.android.gradle.sourcecontext

import org.gradle.api.Project

internal const val PROJECT_DIR = "intermediates/sentry"
internal const val ROOT_DIR = "sentry"

class OutputPaths(private val project: Project, variantName: String) {
  private fun file(path: String) = project.layout.buildDirectory.file(path)

  private fun dir(path: String) = project.layout.buildDirectory.dir(path)

  private val variantDirectory = "$PROJECT_DIR/$variantName"

  val proguardUuidDir = dir("$variantDirectory/proguard-uuid")
  val bundleIdDir = dir("$variantDirectory/bundle-id")
  val explodedSourcesPath = file("$variantDirectory/exploded-sources/exploded-sources.txt")
}

class RootOutputPaths(private val project: Project) {
  private fun file(path: String) = project.layout.buildDirectory.file(path)
  private fun dir(path: String) = project.layout.buildDirectory.dir(path)

  val sourceDir = dir("$ROOT_DIR/source-to-bundle")
  val bundleDir = dir("$ROOT_DIR/source-bundle")
}
