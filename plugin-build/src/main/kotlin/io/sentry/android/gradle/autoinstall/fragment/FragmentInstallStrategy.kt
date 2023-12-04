package io.sentry.android.gradle.autoinstall.fragment

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule // TODO: make it cacheable somehow (probably depends on parameters)
abstract class FragmentInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_FRAGMENT_ID

    override val minSupportedSentryVersion: SemVer get() = SemVer(5, 1, 0)

    companion object Registrar : InstallStrategyRegistrar {
        private const val FRAGMENT_GROUP = "androidx.fragment"
        private const val FRAGMENT_ID = "fragment"
        internal const val SENTRY_FRAGMENT_ID = "sentry-android-fragment"

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$FRAGMENT_GROUP:$FRAGMENT_ID",
                FragmentInstallStrategy::class.java
            ) {}
        }
    }
}
