package io.sentry.android.gradle

import io.sentry.android.gradle.util.GroovyCompat.isDexguardAvailable
import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import io.sentry.gradle.common.SentryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal object SentryTasksProvider {

  /**
   * Returns the minify task for the given project and variant. It could be either ProGuard, R8 or
   * DexGuard.
   *
   * @return the task or null otherwise
   */
  @JvmStatic
  fun getMinifyTask(
    project: Project,
    variantName: String,
    dexguardEnabled: Boolean = false,
  ): TaskProvider<Task>? {
    val tasks =
      if (dexguardEnabled) {
        // We prioritize the Guardsquare's Proguard task towards the AGP ones.
        listOf(
          "transformClassesAndResourcesWithProguardTransformFor${variantName.capitalized}",
          "mergeDex${variantName.capitalized}",
        )
      } else {
        listOf(
          "minify${variantName.capitalized}WithR8",
          "minify${variantName.capitalized}WithProguard",
        )
      }
    return project.findTask(tasks)
  }

  /**
   * Returns the Compose mapping merge task for the given project and variant. This task is
   * responsible for merging Compose mapping data with the R8 mapping file. The final mapping file
   * at build/outputs/mapping/<variant>/mapping.txt is written by this task, not by R8 directly.
   *
   * https://github.com/JetBrains/kotlin/blob/b73fc4e8afb382976646fac728e717fd0b1d1c9c/libraries/tools/kotlin-compose-compiler/src/common/kotlin/org/jetbrains/kotlin/compose/compiler/gradle/internal/ComposeAgpMappingFile.kt#L84-L87
   *
   * @return the task or null if the Kotlin Compose plugin is not applied or the task doesn't exist
   */
  @JvmStatic
  fun getComposeMappingMergeTask(project: Project, variantName: String): TaskProvider<Task>? =
    project.findTask(listOf("merge${variantName.capitalized}ComposeMapping"))

  /**
   * Returns the pre bundle task for the given project and variant.
   *
   * @return the task or null otherwise
   */
  @JvmStatic
  fun getPreBundleTask(project: Project, variantName: String): TaskProvider<Task>? =
    project.findTask(listOf("build${variantName.capitalized}PreBundle"))

  /**
   * Returns the pre bundle task for the given project and variant.
   *
   * @return the task or null otherwise
   */
  @JvmStatic
  fun getBundleTask(project: Project, variantName: String): TaskProvider<Task>? =
    project.findTask(listOf("bundle${variantName.capitalized}"))

  /**
   * Returns the package bundle task (App Bundle only)
   *
   * @return the package task or null if not found
   */
  @JvmStatic
  fun getPackageBundleTask(project: Project, variantName: String): TaskProvider<Task>? =
    // for APK it uses getPackageProvider
    project.findTask(listOf("package${variantName.capitalized}Bundle"))

  /**
   * Returns the assemble task provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getAssembleTaskProvider(project: Project, variant: SentryVariant): TaskProvider<out Task>? =
    variant.assembleProvider ?: project.findTask(listOf("assemble${variant.name.capitalized}"))

  /**
   * Returns the install task provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getInstallTaskProvider(project: Project, variant: SentryVariant): TaskProvider<out Task>? =
    variant.installProvider ?: project.findTask(listOf("install${variant.name.capitalized}"))

  /**
   * Returns the mapping file provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getMappingFileProvider(
    project: Project,
    variant: SentryVariant,
    dexguardEnabled: Boolean = false,
  ): Provider<FileCollection> {
    if (dexguardEnabled) {
      val sep = File.separator
      if (project.plugins.hasPlugin("com.guardsquare.proguard")) {
        val fileCollection =
          project.files(
            File(
              project.buildDir,
              "outputs${sep}proguard${sep}${variant.name}${sep}mapping${sep}mapping.txt",
            )
          )
        return project.provider { fileCollection }
      }
      if (isDexguardAvailable(project)) {
        // For DexGuard the mapping file can either be inside the /apk or the /bundle folder
        // (depends on the task that generated it).
        // In addition, newer versions of DexGuard seems
        // to use <flavorName>/<buildType> instead of <flavorName><BuildType>

        val basePath = listOf<String>("outputs", "dexguard", "mapping")

        val fileCollection =
          project.files(
            File(
              project.buildDir,
              basePath.plus(listOf("apk", variant.name, "mapping.txt")).joinToString(sep),
            ),
            File(
              project.buildDir,
              basePath.plus(listOf("bundle", variant.name, "mapping.txt")).joinToString(sep),
            ),
            File(
              project.buildDir,
              basePath
                .plus(listOf("apk", variant.flavorName, variant.buildTypeName, "mapping.txt"))
                .filterNotNull()
                .joinToString(sep),
            ),
            File(
              project.buildDir,
              basePath
                .plus(listOf("bundle", variant.flavorName, variant.buildTypeName, "mapping.txt"))
                .filterNotNull()
                .joinToString(sep),
            ),
          )
        return project.provider { fileCollection }
      }
    }
    return variant.mappingFileProvider(project)
  }

  /**
   * Returns the package provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getPackageProvider(variant: SentryVariant): TaskProvider<out Task>? =
    // for App Bundle it uses getPackageBundleTask
    variant.packageProvider

  /**
   * Returns the lintVitalAnalyze task provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getLintVitalAnalyzeProvider(project: Project, variantName: String) =
    project.findTask(listOf("lintVitalAnalyze${variantName.capitalized}"))

  /**
   * Returns the lintVitalReport task provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getLintVitalReportProvider(project: Project, variantName: String) =
    project.findTask(listOf("lintVitalReport${variantName.capitalized}"))

  /**
   * Returns the processResources task provider
   *
   * @return the provider if found or null otherwise
   */
  @JvmStatic
  fun getProcessResourcesProvider(project: Project) = project.findTask(listOf("processResources"))

  /** @return the first task found in the list or null */
  private fun Project.findTask(taskName: List<String>): TaskProvider<Task>? =
    taskName
      .mapNotNull {
        try {
          project.tasks.named(it)
        } catch (e: UnknownTaskException) {
          null
        }
      }
      .firstOrNull()

  internal val String.capitalized: String
    get() = this.capitalizeUS()
}
