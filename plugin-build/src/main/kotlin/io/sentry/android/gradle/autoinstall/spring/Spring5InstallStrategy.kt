package io.sentry.android.gradle.autoinstall.spring

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class Spring5InstallStrategy : AbstractInstallStrategy {

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
    get() = SENTRY_SPRING_5_ID

  override val minSupportedThirdPartyVersion: SemVer
    get() = MIN_SUPPORTED_VERSION

  override val maxSupportedThirdPartyVersion: SemVer
    get() = MAX_SUPPORTED_VERSION

  override val minSupportedSentryVersion: SemVer
    get() = SemVer(4, 1, 0)

  companion object Registrar : InstallStrategyRegistrar {
    private const val SPRING_GROUP = "org.springframework"
    private const val SPRING_5_ID = "spring-core"
    internal const val SENTRY_SPRING_5_ID = "sentry-spring"

    private val MIN_SUPPORTED_VERSION = SemVer(5, 1, 2)
    private val MAX_SUPPORTED_VERSION = SemVer(5, 9999, 9999)

    override fun register(
      component: ComponentMetadataHandler,
      autoInstallEnabled: Boolean,
      sentryVersion: String,
    ) {
      component.withModule("$SPRING_GROUP:$SPRING_5_ID", Spring5InstallStrategy::class.java) {
        it.params(autoInstallEnabled, sentryVersion)
      }
    }
  }
}
