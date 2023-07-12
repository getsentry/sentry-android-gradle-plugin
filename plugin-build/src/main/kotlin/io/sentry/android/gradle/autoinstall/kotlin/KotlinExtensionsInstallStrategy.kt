package io.sentry.android.gradle.autoinstall.kotlin

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class KotlinExtensionsInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_KOTLIN_EXTENSIONS_ID

    override val shouldInstallModule: Boolean get() =
        AutoInstallState.getInstance().installKotlinExtensions

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    // prior versions would cause circular dependencies
    // due to having kotlin coroutines as implementation dependency
    override val minSupportedSentryVersion: SemVer get() = SemVer(6, 25, 2)

    companion object Registrar : InstallStrategyRegistrar {
        private const val KOTLINX_GROUP = "org.jetbrains.kotlinx"
        private const val KOTLIN_COROUTINES_ID = "kotlinx-coroutines-core"
        internal const val SENTRY_KOTLIN_EXTENSIONS_ID = "sentry-kotlin-extensions"

        private val MIN_SUPPORTED_VERSION = SemVer(1, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$KOTLINX_GROUP:$KOTLIN_COROUTINES_ID",
                KotlinExtensionsInstallStrategy::class.java
            ) {}
        }
    }
}
