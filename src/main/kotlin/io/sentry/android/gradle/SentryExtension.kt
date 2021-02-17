package io.sentry.android.gradle

import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class SentryExtension @Inject internal constructor() {
    abstract val autoProguardConfig: Property<Boolean>
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
        with(autoProguardConfig) {
            convention(true)
            finalizeValueOnRead()
        }
        with(autoUpload) {
            convention(true)
            finalizeValueOnRead()
        }
        with(uploadNativeSymbols) {
            convention(false)
            finalizeValueOnRead()
        }
        with(includeNativeSources) {
            convention(false)
            finalizeValueOnRead()
        }
    }
}