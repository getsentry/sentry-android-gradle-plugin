package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.MergeSourceSetFolders
import com.android.build.gradle.tasks.PackageAndroidArtifact
import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
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
    fun getTransformerTask(project: Project, variantName: String): TaskProvider<Task>? =
        project.findTask(
            // AGP 3.3 includes the R8 shrinker.
            "minify${variantName.capitalized}WithR8",
            "minify${variantName.capitalized}WithProguard"
        )

    /**
     * Returns the pre bundle task for the given project and variant.
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getPreBundleTask(project: Project, variantName: String): TaskProvider<Task>? =
        project.findTask("build${variantName.capitalized}PreBundle")

    /**
     * Returns the pre bundle task for the given project and variant.
     *
     * @return the task or null otherwise
     */
    @JvmStatic
    fun getBundleTask(project: Project, variantName: String): TaskProvider<Task>? =
        project.findTask("bundle${variantName.capitalized}")

    /**
     * Returns the package bundle task (App Bundle only)
     *
     * @return the package task or null if not found
     */
    @JvmStatic
    fun getPackageBundleTask(project: Project, variantName: String): TaskProvider<Task>? =
        // for APK it uses getPackageProvider
        project.findTask(
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

    /**
     * Returns the mapping file provider
     *
     * @return the provider if found or null otherwise
     */
    @JvmStatic
    fun getMappingFileProvider(variant: ApplicationVariant): Provider<FileCollection> =
        variant.mappingFileProvider

    /**
     * Returns the package provider
     *
     * @return the provider if found or null otherwise
     */
    @JvmStatic
    fun getPackageProvider(variant: ApplicationVariant): TaskProvider<PackageAndroidArtifact>? =
        // for App Bundle it uses getPackageBundleTask
        variant.packageApplicationProvider

    private fun Project.findTask(vararg taskName: String): TaskProvider<Task>? =
        taskName
            .mapNotNull {
                try {
                    project.tasks.named(it)
                } catch (e: UnknownTaskException) {
                    null
                }
            }
            .firstOrNull()

    private val String.capitalized: String get() = this.capitalizeUS()
}
