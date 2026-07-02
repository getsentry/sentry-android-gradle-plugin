package io.sentry.android.gradle.autoinstall.compose

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

abstract class ComposeInstallStrategy : AbstractInstallStrategy {

  constructor(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
    logger: Logger,
  ) : super(autoInstallEnabled, sentryVersion) {
    this.logger = logger
  }

  @Suppress("unused") // used by Gradle
  @Inject // inject is needed to avoid Gradle error
  constructor(
    autoInstallEnabled: Boolean,
    sentryVersion: String,
  ) : this(autoInstallEnabled, sentryVersion, SentryPlugin.logger)

  override val sentryModuleId: String
    get() = SENTRY_COMPOSE_ID

  override val minSupportedSentryVersion: SemVer
    get() = SemVer(6, 7, 0)

  override val minSupportedThirdPartyVersion: SemVer
    get() = SemVer(1, 0, 0)

  companion object Registrar : InstallStrategyRegistrar {

    internal const val SENTRY_COMPOSE_ID = "sentry-compose-android"

    override fun register(
      component: ComponentMetadataHandler,
      autoInstallEnabled: Boolean,
      sentryVersion: String,
    ) {
      component.withModule("androidx.compose.runtime:runtime", ComposeInstallStrategy::class.java) {
        it.params(autoInstallEnabled, sentryVersion)
      }
    }
  }
}
