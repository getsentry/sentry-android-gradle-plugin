package io.sentry.android.gradle.autoinstall.quartz

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class QuartzInstallStrategy : AbstractInstallStrategy {

  constructor(logger: Logger) : super() {
    this.logger = logger
  }

  @Suppress("unused") // used by Gradle
  @Inject // inject is needed to avoid Gradle error
  constructor() : this(SentryPlugin.logger)

  override val sentryModuleId: String
    get() = SENTRY_QUARTZ_ID

  override val minSupportedSentryVersion: SemVer
    get() = SemVer(6, 30, 0)

  companion object Registrar : InstallStrategyRegistrar {
    private const val QUARTZ_GROUP = "org.quartz-scheduler"
    private const val QUARTZ_ID = "quartz"
    internal const val SENTRY_QUARTZ_ID = "sentry-quartz"

    override fun register(component: ComponentMetadataHandler) {
      component.withModule("$QUARTZ_GROUP:$QUARTZ_ID", QuartzInstallStrategy::class.java) {}
    }
  }
}
