package io.sentry.android.gradle

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class SentryPluginExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    /**
     * Whether the plugin should attempt to auto-upload the mapping file to Sentry or not.
     * If disabled the plugin will run a dry-run.
     * Default is enabled.
     */
    val autoUpload: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Disables or enables the automatic configuration of Native Symbols
     * for Sentry. This executes sentry-cli automatically so
     * you don't need to do it manually.
     * Default is disabled.
     */
    val uploadNativeSymbols: Property<Boolean> = objects.property(Boolean::class.java).convention(
        false
    )

    /**
     * Includes or not the source code of native code for Sentry.
     * This executes sentry-cli with the --include-sources param. automatically so
     * you don't need to do it manually.
     * Default is disabled.
     */
    val includeNativeSources: Property<Boolean> = objects.property(Boolean::class.java).convention(
        false
    )

    /** List of Android build variants that should be ignored by the Sentry plugin. */
    val ignoredVariants: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    /** List of Android build types that should be ignored by the Sentry plugin. */
    val ignoredBuildTypes: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    /** List of Android build flavors that should be ignored by the Sentry plugin. */
    val ignoredFlavors: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    /**
     * Forces dependencies instrumentation, even if they were already instrumented.
     * Useful when there are issues with code instrumentation, e.g. the dependencies are
     * partially instrumented.
     * Defaults to false.
     */
    val forceInstrumentDependencies: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /**
     * Enabled debug output of the plugin. Useful when there are issues with code instrumentation,
     * shows the modified bytecode.
     * Defaults to false.
     */
    val debugInstrumentation: Property<Boolean> = objects.property(Boolean::class.java).convention(
        false
    )
}
