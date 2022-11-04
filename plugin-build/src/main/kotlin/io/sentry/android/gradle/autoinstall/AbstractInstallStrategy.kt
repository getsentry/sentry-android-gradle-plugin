package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.slf4j.Logger

abstract class AbstractInstallStrategy : ComponentMetadataRule {

    protected lateinit var logger: Logger

    protected abstract val sentryModuleId: String

    protected abstract val shouldInstallModule: Boolean

    protected open val minSupportedThirdPartyVersion: SemVer = SemVer(0, 0, 0)

    protected open val minSupportedSentryVersion: SemVer = SemVer(0, 0, 0)

    override fun execute(context: ComponentMetadataContext) {
        val autoInstallState = AutoInstallState.getInstance()
        if (!shouldInstallModule) {
            logger.info {
                "$sentryModuleId won't be installed because it was already installed directly"
            }
            return
        }
        val thirdPartySemVersion = SemVer.parse(context.details.id.version)
        if (thirdPartySemVersion < minSupportedThirdPartyVersion) {
            logger.warn {
                "$sentryModuleId won't be installed because the current version is " +
                    "lower than the minimum supported version ($minSupportedThirdPartyVersion)"
            }
            return
        }

        val sentrySemVersion = SemVer.parse(autoInstallState.sentryVersion)
        if (sentrySemVersion < minSupportedSentryVersion) {
            logger.warn {
                "$sentryModuleId won't be installed because the current version is lower than " +
                    "the minimum supported sentry version ($autoInstallState.sentryVersion)"
            }
            return
        }

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.sentryVersion
                dependencies.add("$SENTRY_GROUP:$sentryModuleId:$sentryVersion")

                logger.info {
                    "$sentryModuleId was successfully installed with version: $sentryVersion"
                }
            }
        }
    }
}
