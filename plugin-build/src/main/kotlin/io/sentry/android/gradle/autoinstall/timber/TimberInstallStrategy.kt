package io.sentry.android.gradle.autoinstall.timber

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.util.info
import javax.inject.Inject
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler

//@CacheableRule
abstract class TimberInstallStrategy @Inject constructor(
    private val autoInstallState: AutoInstallState
) : ComponentMetadataRule {

    override fun execute(context: ComponentMetadataContext) {
        if (!autoInstallState.installTimber) {
            SentryPlugin.logger.info {
                "$SENTRY_TIMBER_ID won't be installed because it was already installed directly"
            }
            return
        }

        // TODO: technically we do not support version < 4.6.0 of Timber, because they introduced
        // nullability annotations for Kotlin, so if somebody used to extend Timber.Tree in their
        // kotlin codebase, after adding sentry-android-timber the code won't be compile-able (source-incompatible)
        // because we bring-in a transitive version 4.7.1. However, I think it's a fairly small
        // price to pay, the users would need to remove a single `?` to make one parameter non-nullable and make it work.
        // On the other hand version 4.6.0 is nearly 5 years old, so probably it's safe to drop4
        // everything below that version.

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.sentryVersion
                dependencies.add("$SENTRY_GROUP:$SENTRY_TIMBER_ID:$sentryVersion")

                SentryPlugin.logger.info {
                    "$SENTRY_TIMBER_ID is successfully installed with version: $sentryVersion"
                }
            }
        }
    }

    companion object Registrar : InstallStrategyRegistrar {
        private const val TIMBER_GROUP = "com.jakewharton.timber"
        private const val TIMBER_ID = "timber"
        internal const val SENTRY_TIMBER_ID = "sentry-android-timber"

        override fun register(component: ComponentMetadataHandler) {
            component.withModule("$TIMBER_GROUP:$TIMBER_ID", TimberInstallStrategy::class.java) {
                it.params(AutoInstallState)
            }
        }
    }
}
