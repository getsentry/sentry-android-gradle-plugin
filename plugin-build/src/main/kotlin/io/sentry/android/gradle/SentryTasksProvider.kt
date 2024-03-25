package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.MergeSourceSetFolders
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
     * Returns the transformer task for the given project and variant.
     * It could be either ProGuard or R8
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getPreBuildTask(
        project: Project,
        variantName: String
    ): TaskProvider<Task>? = project.findTask(
        listOf(
            "pre${variantName.capitalized}Build",
        )
    )

    /**
     * Returns the transformer task for the given project and variant.
     * It could be either ProGuard or R8
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getMinifyTasks(
        project: Project,
        variantName: String,
        dexguardEnabled: Boolean = false
    ): List<TaskProvider<Task>> {
        if (dexguardEnabled) {
            // We prioritize the Guardsquare's Proguard task towards the AGP ones.
            /* ktlint-disable max-line-length */
            val transformTask = project.findTask(
                listOf(
                    "transformClassesAndResourcesWithProguardTransformFor${variantName.capitalized}"
                )
            )
            /* ktlint-enable max-line-length */
            if (transformTask != null) {
                return listOf(transformTask)
            }

            // return both apk/aab tasks
            return project.findTasks(
                listOf(
                    "dexguardApk${variantName.capitalized}",
                    "dexguardAab${variantName.capitalized}"
                )
            )
        } else {
            // only hook into first task if found
            val task = project.findTask(
                listOf(
                    "minify${variantName.capitalized}WithR8",
                    "minify${variantName.capitalized}WithProguard"
                )
            )
            return task?.let { listOf(task) } ?: emptyList()
        }
    }

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
     * Returns the merge asset provider
     *
     * @return the provider if found or null otherwise
     */
    @JvmStatic
    fun getMergeAssetsProvider(variant: ApplicationVariant): TaskProvider<MergeSourceSetFolders>? =
        variant.mergeAssetsProvider

    /**
     * Returns the mapping file provider
     *
     * @return the provider if found or null otherwise
     */
    @JvmStatic
    fun getMappingFileProvider(
        project: Project,
        variant: SentryVariant,
        dexguardEnabled: Boolean = false
    ): Provider<FileCollection> {
        if (dexguardEnabled) {
            val sep = File.separator
            if (project.plugins.hasPlugin("com.guardsquare.proguard")) {
                val fileCollection = project.files(
                    File(
                        project.buildDir,
                        "outputs${sep}proguard${sep}${variant.name}${sep}mapping${sep}mapping.txt"
                    )
                )
                return project.provider { fileCollection }
            }
            if (isDexguardAvailable(project)) {
                // For DexGuard the mapping file can either be inside the /apk or the /bundle folder
                // (depends on the task that generated it).
                val mappingDir = "outputs${sep}dexguard${sep}mapping$sep"
                val fileCollection = project.files(
                    File(
                        project.buildDir,
                        "${mappingDir}apk${sep}${variant.name}${sep}mapping.txt"
                    ),
                    File(
                        project.buildDir,
                        "${mappingDir}bundle${sep}${variant.name}${sep}mapping.txt"
                    )
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

    /**
     * @return the first task found in the list or null
     */
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

    /**
     * @return all tasks found in the list or null
     */
    private fun Project.findTasks(taskName: List<String>): List<TaskProvider<Task>> =
        taskName.mapNotNull {
            try {
                project.tasks.named(it)
            } catch (e: UnknownTaskException) {
                null
            }
        }

    internal val String.capitalized: String get() = this.capitalizeUS()
}
