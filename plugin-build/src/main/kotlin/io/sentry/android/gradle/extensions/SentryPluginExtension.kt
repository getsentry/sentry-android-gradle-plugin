package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class SentryPluginExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    /**
     * Disables or enables the handling of Proguard mapping for Sentry.
     * If enabled the plugin will generate a UUID and will take care of
     * uploading the mapping to Sentry. If disabled, all the logic
     * related to proguard mapping will be excluded.
     * Default is enabled.
     *
     * @see [autoUpload]
     * @see [autoUploadProguardMapping]
     */
    val includeProguardMapping: Property<Boolean> = objects
        .property(Boolean::class.java).convention(true)

    /**
     * Whether the plugin should attempt to auto-upload the mapping file to Sentry or not.
     * If disabled the plugin will run a dry-run.
     * Default is enabled.
     */
    val autoUploadProguardMapping: Property<Boolean> = objects
        .property(Boolean::class.java).convention(true)

    /**
     * Whether the plugin should attempt to auto-upload the mapping file to Sentry or not.
     * If disabled the plugin will run a dry-run.
     * Default is enabled.
     */
    @Deprecated(
        "Use autoUploadProguardMapping instead",
        replaceWith = ReplaceWith("autoUploadProguardMapping")
    )
    val autoUpload: Property<Boolean> = autoUploadProguardMapping

    /**
     * Disables or enables the automatic configuration of Native Symbols
     * for Sentry. This executes sentry-cli automatically so
     * you don't need to do it manually.
     * Default is disabled.
     *
     * @see [autoUploadNativeSymbols]
     */
    val uploadNativeSymbols: Property<Boolean> = objects.property(Boolean::class.java).convention(
        false
    )

    /**
     * Whether the plugin should attempt to auto-upload the native debug symbols to Sentry or not.
     * If disabled the plugin will run a dry-run.
     * Default is enabled.
     */
    val autoUploadNativeSymbols: Property<Boolean> =
        objects.property(Boolean::class.java).convention(true)

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
    val ignoredVariants: SetProperty<String> = objects.setProperty(String::class.java)
        .convention(emptySet())

    /** List of Android build types that should be ignored by the Sentry plugin. */
    val ignoredBuildTypes: SetProperty<String> = objects.setProperty(String::class.java)
        .convention(emptySet())

    /** List of Android build flavors that should be ignored by the Sentry plugin. */
    val ignoredFlavors: SetProperty<String> = objects.setProperty(String::class.java)
        .convention(emptySet())

    val tracingInstrumentation: TracingInstrumentationExtension = objects.newInstance(
        TracingInstrumentationExtension::class.java
    )

    /**
     * Configure the tracing instrumentation.
     * Default configuration is enabled.
     */
    fun tracingInstrumentation(
        tracingInstrumentationAction: Action<TracingInstrumentationExtension>
    ) {
        tracingInstrumentationAction.execute(tracingInstrumentation)
    }

    val autoInstallation: AutoInstallExtension = objects.newInstance(
        AutoInstallExtension::class.java
    )

    /**
     * Configure the auto installation feature.
     */
    fun autoInstallation(
        autoInstallationAction: Action<AutoInstallExtension>
    ) {
        autoInstallationAction.execute(autoInstallation)
    }
}
