// ktlint-disable max-line-length
package io.sentry.android.gradle.autoinstall.override

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.info
import javax.inject.Inject
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

abstract class WarnOnOverrideStrategy : ComponentMetadataRule {

    private var logger: Logger

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override fun execute(context: ComponentMetadataContext) {
        val autoInstallState = AutoInstallState.getInstance()

        if (!autoInstallState.enabled) {
            return
        }

        val providedVersion =
            parseVersionSafely(autoInstallState.sentryVersion) ?: return
        val userVersion = parseVersionSafely(context.details.id.version) ?: return

        if (userVersion < providedVersion) {
            logger.warn(
                "WARNING: Version of '${context.details.id.module}' was overridden from '$userVersion' to '$providedVersion' by the Sentry Gradle plugin. If you want to use the older version, you can add `autoInstallation.sentryVersion.set(\"$userVersion\")` in the `sentry {}` plugin configuration block"
            )
        }
    }

    private fun parseVersionSafely(version: String): SemVer? {
        return try {
            SemVer.parse(version)
        } catch (t: Throwable) {
            logger.info { "Unable to parse version $version as a semantic version." }
            null
        }
    }

    companion object Registrar : InstallStrategyRegistrar {
        private val sentryModules = setOf(
            SentryModules.SENTRY,
            SentryModules.SENTRY_ANDROID,
            SentryModules.SENTRY_ANDROID_CORE,
            SentryModules.SENTRY_ANDROID_NDK,
            SentryModules.SENTRY_ANDROID_OKHTTP,
            SentryModules.SENTRY_ANDROID_SQLITE,
            SentryModules.SENTRY_ANDROID_COMPOSE,
            SentryModules.SENTRY_ANDROID_FRAGMENT,
            SentryModules.SENTRY_ANDROID_NAVIGATION,
            SentryModules.SENTRY_ANDROID_TIMBER,
            SentryModules.SENTRY_KOTLIN_EXTENSIONS,
            SentryModules.SENTRY_GRAPHQL,
            SentryModules.SENTRY_JDBC,
            SentryModules.SENTRY_LOG4J2,
            SentryModules.SENTRY_LOGBACK,
            SentryModules.SENTRY_QUARTZ,
            SentryModules.SENTRY_SPRING5,
            SentryModules.SENTRY_SPRING6,
            SentryModules.SENTRY_SPRING_BOOT2,
            SentryModules.SENTRY_SPRING_BOOT3
        )

        override fun register(component: ComponentMetadataHandler) {
            sentryModules.forEach { module ->
                component.withModule(
                    module.toString(),
                    WarnOnOverrideStrategy::class.java
                ) {}
            }
        }
    }
}
