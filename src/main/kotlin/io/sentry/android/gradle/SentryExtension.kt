package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Optional
import javax.inject.Inject

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class BaseSentryExtension {
    abstract val organization: Property<String>
    abstract val project: Property<String>

    abstract val autoUpload: Property<Boolean>

    /**
     * Disables or enables the automatic configuration of Native Symbols
     * for Sentry. This executes sentry-cli automatically so
     * you don't need to do it manually.
     * Default is disabled.
     */
    abstract val uploadNativeSymbols: Property<Boolean>

    /**
     * Includes or not the source code of native code for Sentry.
     * This executes sentry-cli with the --include-sources param. automatically so
     * you don't need to do it manually.
     * Default is disabled.
     */
    abstract val includeNativeSources: Property<Boolean>

    init {
        organization.finalizeValueOnRead()
        project.finalizeValueOnRead()
        autoUpload.finalizeValueOnRead()
        uploadNativeSymbols.finalizeValueOnRead()
        includeNativeSources.finalizeValueOnRead()
    }
}

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class SentryExtension @Inject internal constructor() : BaseSentryExtension() {
    @get:Optional
    abstract val sentryCli: RegularFileProperty

    abstract val autoProguardConfig: Property<Boolean>

    abstract val variantFilter: Property<Spec<ApplicationVariant>>

    init {
        autoUpload.convention(true)
        uploadNativeSymbols.convention(false)
        includeNativeSources.convention(false)

        sentryCli.finalizeValueOnRead()

        with(autoProguardConfig) {
            convention(true)
            finalizeValueOnRead()
        }

        with(variantFilter) {
            finalizeValueOnRead()
            convention(Spec<ApplicationVariant> {
                it.buildType.isMinifyEnabled && it.buildType.name != "debug"
            })
        }
    }

    fun variantFilter(filter: Spec<ApplicationVariant>) {
        variantFilter.set(filter)
    }
}

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class SentryBuildTypeExtension @Inject constructor() : BaseSentryExtension() {
    abstract val enabled: Property<Boolean>

    init {
        with(enabled) {
            finalizeValueOnRead()
            convention(true)
        }
    }
}