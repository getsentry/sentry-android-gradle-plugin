package io.sentry.android.gradle.autoinstall.graphql

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class GraphqlInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_GRAPHQL_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installGraphql

    // prior versions could cause circular dependencies
    // due to having graphql as implementation dependency
    override val minSupportedSentryVersion: SemVer get() = SemVer(6, 25, 2)

    companion object Registrar : InstallStrategyRegistrar {
        private const val GRAPHQL_GROUP = "com.graphql-java"
        private const val GRAPHQL_ID = "graphql-java"
        internal const val SENTRY_GRAPHQL_ID = "sentry-graphql"

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$GRAPHQL_GROUP:$GRAPHQL_ID",
                GraphqlInstallStrategy::class.java
            ) {}
        }
    }
}
