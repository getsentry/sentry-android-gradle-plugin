package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.MergeSourceSetFolders
import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

internal object SentryTasksProvider {

    /**
     * Returns the transformer task for the given project and variant.
     * It could be either ProGuard or R8
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getTransformerTask(project: Project, variantName: String): Task? =
        project.findTask(
            // Android Studio 3.3 includes the R8 shrinker.
            "transformClassesAndResourcesWithR8For${variantName.capitalized}",
            "transformClassesAndResourcesWithProguardFor${variantName.capitalized}",
            "minify${variantName.capitalized}WithR8",
            "minify${variantName.capitalized}WithProguard"
        )

    /**
     * Returns the dex task for the given project and variant.
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getDexTask(project: Project, variantName: String): Task? =
        project.findTask(
            // Android Studio 3.3 includes the R8 shrinker.
            "transformClassesWithDexFor${variantName.capitalized}",
            "transformClassesWithDexBuilderFor${variantName.capitalized}",
            "transformClassesAndDexWithShrinkResFor${variantName.capitalized}"
        )

    /**
     * Returns the pre bundle task for the given project and variant.
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getPreBundleTask(project: Project, variantName: String): Task? =
        project.findTask("build${variantName.capitalized}PreBundle")

    /**
     * Returns the pre bundle task for the given project and variant.
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getBundleTask(project: Project, variantName: String): Task? =
        project.findTask("bundle${variantName.capitalized}")

    /**
     * Returns the package task
     *
     * @return the package task or null if not found
     */
    @JvmStatic
    fun getPackageTask(project: Project, variantName: String): Task? =
        project.findTask(
            "package${variantName.capitalized}",
            "package${variantName.capitalized}Bundle"
        )

    /**
     * Returns the assemble task provider
     *
     * @return the provider if found or null otherwise
     */
    @JvmStatic
    fun getAssembleTaskProvider(variant: ApplicationVariant): TaskProvider<Task>? =
        variant.assembleProvider

    /**
     * Returns the merge asset provider
     *
     * @return the provider if found or null otherwise
     */
    @JvmStatic
    fun getMergeAssetsProvider(variant: ApplicationVariant): TaskProvider<MergeSourceSetFolders>? =
        variant.mergeAssetsProvider

    private fun Project.findTask(vararg taskName: String): Task? =
        taskName.mapNotNull { project.tasks.findByName(it) }.firstOrNull()

    private val String.capitalized: String get() = this.capitalizeUS()
}
