package io.sentry.android.gradle.autoinstall.log4j2

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class Log4j2InstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_LOG4J2_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installLog4j2

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    override val minSupportedSentryVersion: SemVer get() = SemVer(6, 25, 2)

    companion object Registrar : InstallStrategyRegistrar {
        private const val LOG4J2_GROUP = "org.apache.logging.log4j"
        private const val LOG4J2_ID = "log4j-api"
        internal const val SENTRY_LOG4J2_ID = "sentry-log4j2"

        private val MIN_SUPPORTED_VERSION = SemVer(2, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$LOG4J2_GROUP:$LOG4J2_ID",
                Log4j2InstallStrategy::class.java
            ) {}
        }
    }
}
