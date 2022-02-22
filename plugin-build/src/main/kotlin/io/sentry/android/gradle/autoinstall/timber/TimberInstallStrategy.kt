package io.sentry.android.gradle.autoinstall.timber

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
import org.slf4j.Logger

// @Inject is needed to avoid Gradle error
// @CacheableRule
abstract class TimberInstallStrategy @Inject constructor() : ComponentMetadataRule {

    private var logger: Logger = SentryPlugin.logger

    constructor(logger: Logger) : this() {
        this.logger = logger
    }

    override fun execute(context: ComponentMetadataContext) {
        val autoInstallState = AutoInstallState.getInstance()
        if (!autoInstallState.installTimber) {
            logger.info {
                "$SENTRY_TIMBER_ID won't be installed because it was already installed directly"
            }
            return
        }
        val semVer = SemVer.parse(context.details.id.version)
        if (semVer < MIN_SUPPORTED_VERSION) {
            logger.warn {
                "$SENTRY_TIMBER_ID won't be installed because the current timber version is " +
                    "lower than the minimum supported version ($MIN_SUPPORTED_VERSION)"
            }
            return
        }

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.sentryVersion
                dependencies.add("$SENTRY_GROUP:$SENTRY_TIMBER_ID:$sentryVersion")

                logger.info {
                    "$SENTRY_TIMBER_ID was successfully installed with version: $sentryVersion"
                }
            }
        }
    }

    companion object Registrar : InstallStrategyRegistrar {
        private const val TIMBER_GROUP = "com.jakewharton.timber"
        private const val TIMBER_ID = "timber"
        internal const val SENTRY_TIMBER_ID = "sentry-android-timber"
        private val MIN_SUPPORTED_VERSION = SemVer(4, 6, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$TIMBER_GROUP:$TIMBER_ID",
                TimberInstallStrategy::class.java
            ) {}
        }
    }
}
