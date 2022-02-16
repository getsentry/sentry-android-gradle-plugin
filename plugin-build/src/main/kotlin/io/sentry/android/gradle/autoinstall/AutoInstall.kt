package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy
import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy.Registrar.SENTRY_FRAGMENT_ID
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy.Registrar.SENTRY_OKHTTP_ID
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy.Registrar.SENTRY_TIMBER_ID
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.info
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet

internal const val SENTRY_GROUP = "io.sentry"
private const val SENTRY_ANDROID_ID = "sentry-android"

private val strategies = listOf(
    OkHttpInstallStrategy.Registrar,
    TimberInstallStrategy.Registrar,
    FragmentInstallStrategy.Registrar
)

fun Project.installDependencies(extension: SentryPluginExtension) {
    configurations.named("implementation").configure { configuration ->
        configuration.withDependencies { dependencies ->
            var sentryVersion = dependencies.findSentryAndroidVersion()
            sentryVersion = installSentrySdk(sentryVersion, dependencies, extension)

            val installOkHttp = !dependencies.isModuleAvailable(SENTRY_OKHTTP_ID)
            val installTimber = !dependencies.isModuleAvailable(SENTRY_TIMBER_ID)
            val installFragment = !dependencies.isModuleAvailable(SENTRY_FRAGMENT_ID)
            AutoInstallState.apply {
                this.sentryVersion = sentryVersion
                this.installOkHttp = installOkHttp
                this.installFragment = installFragment
                this.installTimber = installTimber
            }
        }
    }
    project.dependencies.components { component ->
        strategies.forEach { it.register(component) }
    }
}

private fun Project.installSentrySdk(
    sentryVersion: String?,
    dependencies: DependencySet,
    extension: SentryPluginExtension
): String {
    return if (sentryVersion == null) {
        val userDefinedVersion = extension.autoInstallation.sentryVersion.get()
        val sentryAndroidDep =
            this.dependencies.create("$SENTRY_GROUP:$SENTRY_ANDROID_ID:$userDefinedVersion")
        dependencies.add(sentryAndroidDep)
        logger.info {
            "sentry-android is successfully installed with version: $userDefinedVersion"
        }
        userDefinedVersion
    } else {
        logger.info {
            "sentry-android won't be installed because it was already installed directly"
        }
        sentryVersion
    }
}

private fun DependencySet.findSentryAndroidVersion(): String? =
    find { it.group == SENTRY_GROUP && it.name == SENTRY_ANDROID_ID }?.version

private fun DependencySet.isModuleAvailable(id: String): Boolean =
    any { it.group == SENTRY_GROUP && it.name == id }
