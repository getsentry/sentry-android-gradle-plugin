package io.sentry.android.gradle.autoinstall.graphql

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.autoinstall.spring.Spring5InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.Spring5InstallStrategy.Registrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class Graphql22InstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_GRAPHQL_ID

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION
    
    override val minSupportedSentryVersion: SemVer get() = SemVer(8, 0, 0)

    companion object Registrar : InstallStrategyRegistrar {
        private const val GRAPHQL_GROUP = "com.graphql-java"
        private const val GRAPHQL_ID = "graphql-java"
        internal const val SENTRY_GRAPHQL_ID = "sentry-graphql-22"

        private val MIN_SUPPORTED_VERSION = SemVer(22, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$GRAPHQL_GROUP:$GRAPHQL_ID",
                Graphql22InstallStrategy::class.java
            ) {}
        }
    }
}
