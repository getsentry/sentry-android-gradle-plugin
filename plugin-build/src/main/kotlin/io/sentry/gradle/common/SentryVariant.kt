package io.sentry.gradle.common

import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Common interface to be used from the main source set to access Android/JVM variants. Its
 * implementations live within the compatibility source sets.
 */
interface SentryVariant {
  val name: String
  val flavorName: String?
  val buildTypeName: String?
  val productFlavors: List<String>
  val isMinifyEnabled: Boolean
  val isDebuggable: Boolean
  val packageProvider: TaskProvider<out Task>?
    get() = null

  val assembleProvider: TaskProvider<out Task>?
    get() = null

  val installProvider: TaskProvider<out Task>?
    get() = null

  fun mappingFileProvider(project: Project): Provider<FileCollection>

  fun sources(
    project: Project,
    additionalSources: Provider<out Collection<Directory>>,
  ): Provider<out Collection<Directory>>

  fun wireMappingFileToUuidTask(
    project: Project,
    task: TaskProvider<out SentryGenerateProguardUuidTask>,
    variantName: String,
    dexguardEnabled: Boolean,
  )
}

fun Collection<Directory>.filterBuildConfig(): Collection<Directory> = filterNot {
  // consider also AGP buildConfig folder as well as community plugins:
  // https://github.com/yshrsmz/BuildKonfig/blob/727f4f9e79e6726ab9489499ec6d92b6f6d56266/buildkonfig-gradle-plugin/src/main/kotlin/com/codingfeline/buildkonfig/gradle/BuildKonfigPlugin.kt#L47
  // https://github.com/gmazzo/gradle-buildconfig-plugin/blob/9fe73852fe5d545f7825826d288d7b2f4e336060/plugin/src/main/kotlin/com/github/gmazzo/buildconfig/BuildConfigPlugin.kt#L119
  // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/variant/VariantPathHelper.kt;l=274-275?q=buildConfigSourceOutputDir
  it.asFile.path.contains("buildConfig") || it.asFile.path.contains("buildkonfig")
}
