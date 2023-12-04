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

    protected open val minSupportedThirdPartyVersion: SemVer? = null

    protected open val maxSupportedThirdPartyVersion: SemVer? = null

    protected open val minSupportedSentryVersion: SemVer = SemVer(0, 0, 0)

    override fun execute(context: ComponentMetadataContext) {
        val autoInstallState = AutoInstallState.getInstance()
        if (!autoInstallState.enabled) {
            logger.info {
                "$sentryModuleId won't be installed because autoInstallation is disabled"
            }
            return
        }
        minSupportedThirdPartyVersion?.let {
            parseVersion(context.details.id.version)?.let { thirdPartySemVersion ->
                if (thirdPartySemVersion < it) {
                    logger.info {
                        "$sentryModuleId won't be installed because the current version is " +
                            "lower than the minimum supported version ($it)"
                    }
                    return
                }
            } ?: return
        }
        maxSupportedThirdPartyVersion?.let {
            parseVersion(context.details.id.version)?.let { thirdPartySemVersion ->
                if (thirdPartySemVersion > it) {
                    logger.info {
                        "$sentryModuleId won't be installed because the current version is " +
                            "higher than the maximum supported version ($it)"
                    }
                    return
                }
            } ?: return
        }

        if (minSupportedSentryVersion.major > 0) {
            try {
                val sentrySemVersion = SemVer.parse(autoInstallState.sentryVersion)
                if (sentrySemVersion < minSupportedSentryVersion) {
                    logger.warn {
                        "$sentryModuleId won't be installed because the current version is " +
                            "lower than the minimum supported sentry version " +
                            "($autoInstallState.sentryVersion)"
                    }
                    return
                }
            } catch (ex: IllegalArgumentException) {
                logger.warn {
                    "$sentryModuleId won't be installed because the provided " +
                        "sentry version(${autoInstallState.sentryVersion}) could not be " +
                        "processed as a semantic version."
                }
                return
            }
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

    private fun parseVersion(version: String): SemVer? {
        // older Spring versions ended in .RELEASE
        return parseVersionSafely(version.removeSuffix(".RELEASE"))
    }

    private fun parseVersionSafely(version: String): SemVer? {
        try {
            return SemVer.parse(version)
        } catch (t: Throwable) {
            logger.warn { "Unable to parse version $version as a semantic version." }
            return null
        }
    }
}
