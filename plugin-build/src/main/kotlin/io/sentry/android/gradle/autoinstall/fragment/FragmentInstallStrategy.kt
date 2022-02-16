package io.sentry.android.gradle.autoinstall.fragment

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.autoinstall.SENTRY_GROUP
import io.sentry.android.gradle.util.info
import javax.inject.Inject
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler

//@CacheableRule
abstract class FragmentInstallStrategy @Inject constructor(
    private val autoInstallState: AutoInstallState
) : ComponentMetadataRule {

    override fun execute(context: ComponentMetadataContext) {
        if (!autoInstallState.installFragment) {
            SentryPlugin.logger.info {
                "$SENTRY_FRAGMENT_ID won't be installed because it was already installed directly"
            }
            return
        }

        // TODO: technically we support all versions of androidx.fragment, but there can be a case
        // when we bump the version of androidx.fragment in the sentry-android SDK to 1.4+, which
        // requires compileSdkVersion 31+, so the user's app might stop compiling. Should we make the
        // transitive dependency of androidx.fragment `compileOnly` in the sentry-android SDK to avoid this?
        // Another option: we could check the user's compileSdkVersion and do not add sentry-android-fragment
        // (this is, when we bump the transitive androidx.fragment version there to 1.4.1, so
        // no action item for now)

        context.details.allVariants { metadata ->
            metadata.withDependencies { dependencies ->
                val sentryVersion = autoInstallState.sentryVersion
                dependencies.add("$SENTRY_GROUP:$SENTRY_FRAGMENT_ID:$sentryVersion")

                SentryPlugin.logger.info {
                    "$SENTRY_FRAGMENT_ID is successfully installed with version: $sentryVersion"
                }
            }
        }
    }

    companion object Registrar : InstallStrategyRegistrar {
        private const val FRAGMENT_GROUP = "androidx.fragment"
        private const val FRAGMENT_ID = "fragment"
        internal const val SENTRY_FRAGMENT_ID = "sentry-android-fragment"

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$FRAGMENT_GROUP:$FRAGMENT_ID",
                FragmentInstallStrategy::class.java
            ) {
                it.params(AutoInstallState)
            }
        }
    }
}
