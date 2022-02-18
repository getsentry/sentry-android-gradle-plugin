package io.sentry.android.gradle.autoinstall.fragment

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.util.info
import javax.inject.Inject
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.provider.Provider
import org.slf4j.Logger

// @CacheableRule
abstract class FragmentInstallStrategy @Inject constructor(
    private val autoInstallState: Provider<AutoInstallState>
) : ComponentMetadataRule {

    private var logger: Logger = SentryPlugin.logger

    constructor(
        autoInstallState: Provider<AutoInstallState>,
        logger: Logger
    ) : this(autoInstallState) {
        this.logger = logger
    }

    override fun execute(context: ComponentMetadataContext) {
        if (!autoInstallState.get().installFragment) {
            logger.info {
                "$SENTRY_FRAGMENT_ID won't be installed because it was already installed directly"
            }
            return
        }

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.get().sentryVersion
                dependencies.add("$SENTRY_GROUP:$SENTRY_FRAGMENT_ID:$sentryVersion")

                logger.info {
                    "$SENTRY_FRAGMENT_ID is successfully installed with version: $sentryVersion"
                }
            }
        }
    }

    companion object Registrar : InstallStrategyRegistrar {
        private const val FRAGMENT_GROUP = "androidx.fragment"
        private const val FRAGMENT_ID = "fragment"
        internal const val SENTRY_FRAGMENT_ID = "sentry-android-fragment"

        override fun register(
            component: ComponentMetadataHandler,
            autoInstallState: Provider<AutoInstallState>
        ) {
            component.withModule(
                "$FRAGMENT_GROUP:$FRAGMENT_ID",
                FragmentInstallStrategy::class.java
            ) {
                it.params(autoInstallState)
            }
        }
    }
}
