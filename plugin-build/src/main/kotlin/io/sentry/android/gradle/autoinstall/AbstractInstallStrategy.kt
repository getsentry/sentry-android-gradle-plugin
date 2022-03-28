package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.slf4j.Logger

abstract class AbstractInstallStrategy : ComponentMetadataRule {

    protected lateinit var logger: Logger

    protected abstract val moduleId: String

    protected abstract val shouldInstallModule: Boolean

    protected open val minSupportedVersion: SemVer = SemVer(0, 0, 0)

    override fun execute(context: ComponentMetadataContext) {
        val autoInstallState = AutoInstallState.getInstance()
        if (!shouldInstallModule) {
            logger.info {
                "$moduleId won't be installed because it was already installed directly"
            }
            return
        }
        val semVer = SemVer.parse(context.details.id.version)
        if (semVer < minSupportedVersion) {
            logger.warn {
                "$moduleId won't be installed because the current version is " +
                    "lower than the minimum supported version ($minSupportedVersion)"
            }
            return
        }

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.sentryVersion
                dependencies.add("$SENTRY_GROUP:$moduleId:$sentryVersion")

                logger.info {
                    "$moduleId was successfully installed with version: $sentryVersion"
                }
            }
        }
    }
}
