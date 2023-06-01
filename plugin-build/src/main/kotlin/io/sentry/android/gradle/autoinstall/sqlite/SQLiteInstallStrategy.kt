package io.sentry.android.gradle.autoinstall.sqlite

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger
import javax.inject.Inject

abstract class SQLiteInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_SQLITE_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installSqlite

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    override val minSupportedSentryVersion: SemVer get() = SemVer(6, 21, 0)

    companion object Registrar : InstallStrategyRegistrar {
        private const val SQLITE_GROUP = "androidx.sqlite"
        private const val SQLITE_ID = "sqlite"
        internal const val SENTRY_SQLITE_ID = "sentry-android-sqlite"

        private val MIN_SUPPORTED_VERSION = SemVer(2, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$SQLITE_GROUP:$SQLITE_ID",
                SQLiteInstallStrategy::class.java
            ) {}
        }
    }
}
