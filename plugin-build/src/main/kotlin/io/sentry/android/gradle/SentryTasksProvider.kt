package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.Task

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
    fun getPackageTask(project: Project, variantName: String) =
        project.findTask(
            "package${variantName.capitalized}",
            "package${variantName.capitalized}Bundle"
        )

    /**
     * Returns the assemble task
     *
     * @return the task if found or null otherwise
     */
    @JvmStatic
    fun getAssembleTask(variant: ApplicationVariant): Task =
        variant.assembleProvider.get()

    private fun Project.findTask(vararg taskName: String): Task? =
        taskName.mapNotNull { project.tasks.findByName(it) }.firstOrNull()

    private val String.capitalized: String get() = this.capitalize()
}
