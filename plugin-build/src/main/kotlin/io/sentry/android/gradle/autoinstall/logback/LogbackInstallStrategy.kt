package io.sentry.android.gradle.autoinstall.logback

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class LogbackInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_LOGBACK_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installLogback

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    override val minSupportedSentryVersion: SemVer get() = SemVer(4, 1, 0)

    companion object Registrar : InstallStrategyRegistrar {
        private const val LOGBACK_GROUP = "ch.qos.logback"
        private const val LOGBACK_ID = "logback-classic"
        internal const val SENTRY_LOGBACK_ID = "sentry-logback"

        private val MIN_SUPPORTED_VERSION = SemVer(1, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$LOGBACK_GROUP:$LOGBACK_ID",
                LogbackInstallStrategy::class.java
            ) {}
        }
    }
}
