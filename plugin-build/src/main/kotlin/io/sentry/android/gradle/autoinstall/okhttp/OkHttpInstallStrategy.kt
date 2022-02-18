package io.sentry.android.gradle.autoinstall.okhttp

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import javax.inject.Inject
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.provider.Provider
import org.slf4j.Logger

//@CacheableRule
abstract class OkHttpInstallStrategy @Inject constructor(
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
        if (!autoInstallState.get().installOkHttp) {
            logger.info {
                "sentry-android-okhttp won't be installed because it was already installed directly"
            }
            return
        }
        val semVer = SemVer.parse(context.details.id.version)
        if (semVer < MIN_SUPPORTED_VERSION) {
            logger.warn {
                "$SENTRY_OKHTTP_ID won't be installed because the current okhttp version is " +
                    "lower than the minimum supported version ($MIN_SUPPORTED_VERSION)"
            }
            return
        }

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.get().sentryVersion
                dependencies.add("$SENTRY_GROUP:$SENTRY_OKHTTP_ID:$sentryVersion")

                logger.info {
                    "$SENTRY_OKHTTP_ID is successfully installed with version: $sentryVersion"
                }
            }
        }
    }


    companion object Registrar : InstallStrategyRegistrar {
        private const val OKHTTP_GROUP = "com.squareup.okhttp3"
        private const val OKHTTP_ID = "okhttp"
        internal const val SENTRY_OKHTTP_ID = "sentry-android-okhttp"

        private val MIN_SUPPORTED_VERSION = SemVer(3, 13, 0)

        override fun register(
            component: ComponentMetadataHandler,
            autoInstallState: Provider<AutoInstallState>
        ) {
            component.withModule("$OKHTTP_GROUP:$OKHTTP_ID", OkHttpInstallStrategy::class.java) {
                it.params(autoInstallState)
            }
        }
    }
}
