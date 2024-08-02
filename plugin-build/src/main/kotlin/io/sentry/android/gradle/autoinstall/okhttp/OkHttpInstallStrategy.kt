package io.sentry.android.gradle.autoinstall.okhttp

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class OkHttpInstallStrategy : AbstractInstallStrategy {

  constructor(logger: Logger) : super() {
    this.logger = logger
  }

  @Suppress("unused") // used by Gradle
  @Inject // inject is needed to avoid Gradle error
  constructor() : this(SentryPlugin.logger)

  override val sentryModuleId: String
    get() = SENTRY_OKHTTP_ID

  override val minSupportedThirdPartyVersion: SemVer
    get() = MIN_SUPPORTED_VERSION

  override val minSupportedSentryVersion: SemVer
    get() = SemVer(7, 0, 0)

  companion object Registrar : InstallStrategyRegistrar {
    private const val OKHTTP_GROUP = "com.squareup.okhttp3"
    private const val OKHTTP_ID = "okhttp"
    internal const val SENTRY_OKHTTP_ID = "sentry-okhttp"

    private val MIN_SUPPORTED_VERSION = SemVer(3, 13, 0)

    override fun register(component: ComponentMetadataHandler) {
      component.withModule("$OKHTTP_GROUP:$OKHTTP_ID", OkHttpInstallStrategy::class.java) {}
    }
  }
}
