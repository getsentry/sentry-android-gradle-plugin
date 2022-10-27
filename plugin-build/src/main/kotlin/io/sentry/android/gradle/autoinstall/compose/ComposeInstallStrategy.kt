package io.sentry.android.gradle.autoinstall.compose

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

abstract class ComposeInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val moduleId: String get() = SENTRY_COMPOSE_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installCompose

    override val minSupportedVersion: SemVer get() = MIN_SUPPORTED_VERSION

    companion object Registrar : InstallStrategyRegistrar {
        private const val COMPOSE_GROUP = "androidx.compose.runtime"
        private const val COMPOSE_ID = "runtime"
        internal const val SENTRY_COMPOSE_ID = "sentry-compose-android"
        private val MIN_SUPPORTED_VERSION = SemVer(1, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$COMPOSE_GROUP:$COMPOSE_ID",
                ComposeInstallStrategy::class.java
            ) {}
        }
    }
}
