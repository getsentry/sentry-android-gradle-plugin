package io.sentry.android.gradle.autoinstall.spring

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class SpringBoot3InstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_SPRING_BOOT_3_ID

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    override val minSupportedSentryVersion: SemVer get() = SemVer(6, 28, 0)

    companion object Registrar : InstallStrategyRegistrar {
        private const val SPRING_GROUP = "org.springframework.boot"
        private const val SPRING_BOOT_3_ID = "spring-boot"
        internal const val SENTRY_SPRING_BOOT_3_ID = "sentry-spring-boot-jakarta"

        private val MIN_SUPPORTED_VERSION = SemVer(3, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$SPRING_GROUP:$SPRING_BOOT_3_ID",
                SpringBoot3InstallStrategy::class.java
            ) {}
        }
    }
}
