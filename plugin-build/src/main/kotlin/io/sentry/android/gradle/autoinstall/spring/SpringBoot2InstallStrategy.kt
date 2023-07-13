package io.sentry.android.gradle.autoinstall.spring

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class SpringBoot2InstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_SPRING_BOOT_2_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installSpring

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    override val maxSupportedThirdPartyVersion: SemVer get() = MAX_SUPPORTED_VERSION

    override val minSupportedSentryVersion: SemVer get() = SemVer(6, 25, 2)

    companion object Registrar : InstallStrategyRegistrar {
        private const val SPRING_GROUP = "org.springframework.boot"
        private const val SPRING_BOOT_2_ID = "spring-boot-starter"
        internal const val SENTRY_SPRING_BOOT_2_ID = "sentry-spring-boot-starter"

        private val MIN_SUPPORTED_VERSION = SemVer(2, 1, 0)
        private val MAX_SUPPORTED_VERSION = SemVer(2, 9999, 9999)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$SPRING_GROUP:$SPRING_BOOT_2_ID",
                SpringBoot2InstallStrategy::class.java
            ) {}
        }
    }
}
