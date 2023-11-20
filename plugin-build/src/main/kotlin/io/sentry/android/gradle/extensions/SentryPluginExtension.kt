package io.sentry.android.gradle.extensions

import io.sentry.android.gradle.telemetry.SentryTelemetryService.Companion.SENTRY_SAAS_DSN
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
     * Includes or not the source code of native code when uploading native symbols for Sentry.
     * This executes sentry-cli with the --include-sources param. automatically so
     * you don't need to do it manually.
     *
     * This only works with [uploadNativeSymbols] enabled.
     * @see [uploadNativeSymbols]
     *
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
     * Turn on support for GuardSquare's tools integration (Dexguard and External Proguard).
     * If enabled, the plugin will try to consume and upload the mapping file
     * produced by Dexguard and External Proguard.
     * Default is disabled.
     */
    val dexguardEnabled: Property<Boolean> = objects
        .property(Boolean::class.java).convention(false)

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

    /**
     * Disables or enables the reporting of dependencies metadata for Sentry.
     * If enabled the plugin will collect external dependencies and will take care of
     * uploading them to Sentry as part of events. If disabled, all the logic
     * related to dependencies metadata report will be excluded.
     *
     * Default is enabled.
     */
    val includeDependenciesReport: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /**
     * Disables or enables the handling of source context for Sentry.
     * If enabled the plugin will generate a UUID and will take care of
     * uploading the source context to Sentry. If disabled, all the logic
     * related to source context will be excluded.
     * Default is disabled.
     *
     * @see [autoUploadSourceContext]
     */
    val includeSourceContext: Property<Boolean> = objects
        .property(Boolean::class.java).convention(false)

    /**
     * Whether the plugin should attempt to auto-upload the source context to Sentry or not.
     * If disabled the plugin will run a dry-run.
     * Default is enabled.
     */
    val autoUploadSourceContext: Property<Boolean> = objects
        .property(Boolean::class.java).convention(true)

    /**
     * Configure additional directories to be included in the source bundle which is used for
     * source context. The directories should be specified relative to the Gradle module/project's
     * root. For example, if you have a custom source set alongside 'main', the parameter would be
     * 'src/custom/java'.
     */
    val additionalSourceDirsForSourceContext: SetProperty<String> = objects.setProperty(
        String::class.java
    ).convention(
        emptySet()
    )

    /**
     * Disables or enables debug log output, e.g. for for sentry-cli.
     *
     * Default is disabled.
     */
    val debug: Property<Boolean> = objects.property<Boolean?>(Boolean::class.java)
        .convention(false)

    /**
     * The slug of the Sentry organization to use for uploading proguard mappings/source contexts.
     *
     * Default is null.
     */
    val org: Property<String> = objects.property(String::class.java)
        .convention(null as String?)

    /**
     * The slug of the Sentry project to use for uploading proguard mappings/source contexts.
     *
     * Default is null.
     */
    val projectName: Property<String> = objects.property(String::class.java)
        .convention(null as String?)

    /**
     * The authentication token to use for uploading proguard mappings/source contexts.
     * WARNING: Do not expose this token in your build.gradle files, but rather set an environment
     * variable and read it into this property.
     *
     * Default is null.
     */
    val authToken: Property<String> = objects.property(String::class.java)
        .convention(null as String?)

    /**
     * The url of your Sentry instance. If you're using SAAS (not self hosting) you do not have to
     * set this. If you are self hosting you can set your URL here.
     *
     * Default is null meaning Sentry SAAS.
     */
    val url: Property<String> = objects.property(String::class.java)
        .convention(null as String?)

    /**
     * Whether the plugin should send telemetry data to Sentry.
     * If disabled the plugin will not send telemetry data.
     * This is auto disabled if running against a self hosted instance of Sentry.
     * Default is enabled.
     */
    val telemetry: Property<Boolean> = objects
        .property(Boolean::class.java).convention(false)

    /**
     * The DSN (Sentry URL) telemetry data is sent to.
     *
     * Default is Sentry SAAS.
     */
    val telemetryDsn: Property<String> = objects.property(String::class.java)
        .convention(SENTRY_SAAS_DSN)
}
