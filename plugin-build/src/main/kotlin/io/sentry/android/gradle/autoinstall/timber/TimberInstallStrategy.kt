package io.sentry.android.gradle.autoinstall.timber

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class TimberInstallStrategy : AbstractInstallStrategy {

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
    get() = SENTRY_TIMBER_ID

  override val minSupportedThirdPartyVersion: SemVer
    get() = MIN_SUPPORTED_VERSION

  override val minSupportedSentryVersion: SemVer
    get() = SemVer(3, 0, 0)

  companion object Registrar : InstallStrategyRegistrar {
    private const val TIMBER_GROUP = "com.jakewharton.timber"
    private const val TIMBER_ID = "timber"
    internal const val SENTRY_TIMBER_ID = "sentry-android-timber"
    private val MIN_SUPPORTED_VERSION = SemVer(4, 6, 0)

    override fun register(
      component: ComponentMetadataHandler,
      autoInstallEnabled: Boolean,
      sentryVersion: String,
    ) {
      component.withModule("$TIMBER_GROUP:$TIMBER_ID", TimberInstallStrategy::class.java) {
        it.params(autoInstallEnabled, sentryVersion)
      }
    }
  }
}
