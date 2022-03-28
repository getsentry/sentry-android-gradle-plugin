package io.sentry.android.gradle.autoinstall.timber

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class TimberInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val moduleId: String get() = SENTRY_TIMBER_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installTimber

    override val minSupportedVersion: SemVer get() = MIN_SUPPORTED_VERSION

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
