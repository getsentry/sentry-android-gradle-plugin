package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.autoinstall.compose.ComposeInstallStrategy
import io.sentry.android.gradle.autoinstall.compose.ComposeInstallStrategy.Registrar.SENTRY_COMPOSE_ID
import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy
import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy.Registrar.SENTRY_FRAGMENT_ID
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy.Registrar.SENTRY_OKHTTP_ID
import io.sentry.android.gradle.autoinstall.sqlite.SQLiteInstallStrategy
import io.sentry.android.gradle.autoinstall.sqlite.SQLiteInstallStrategy.Registrar.SENTRY_SQLITE_ID
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy.Registrar.SENTRY_TIMBER_ID
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.info
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet

internal const val SENTRY_GROUP = "io.sentry"
private const val SENTRY_ANDROID_ID = "sentry-android"
private const val SENTRY_ANDROID_CORE_ID = "sentry-android-core"

private val strategies = listOf(
    OkHttpInstallStrategy.Registrar,
    SQLiteInstallStrategy.Registrar,
    TimberInstallStrategy.Registrar,
    FragmentInstallStrategy.Registrar,
    ComposeInstallStrategy.Registrar
)

fun Project.installDependencies(extension: SentryPluginExtension) {
    configurations.named("implementation").configure { configuration ->
        configuration.withDependencies { dependencies ->
            // if autoInstallation is disabled, the autoInstallState will contain initial values
            // which all default to false, hence, the integrations won't be installed as well
            if (extension.autoInstallation.enabled.get()) {
                val sentryVersion = dependencies.findSentryAndroidVersion()
                with(AutoInstallState.getInstance(gradle)) {
                    this.sentryVersion = installSentrySdk(sentryVersion, dependencies, extension)

                    installOkHttp = !dependencies.isModuleAvailable(SENTRY_OKHTTP_ID)
                    installSqlite = !dependencies.isModuleAvailable(SENTRY_SQLITE_ID)
                    installTimber = !dependencies.isModuleAvailable(SENTRY_TIMBER_ID)
                    installFragment = !dependencies.isModuleAvailable(SENTRY_FRAGMENT_ID)
                    installCompose = !dependencies.isModuleAvailable(SENTRY_COMPOSE_ID)
                }
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
            "sentry-android was successfully installed with version: $userDefinedVersion"
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
    find {
        it.group == SENTRY_GROUP &&
            (it.name == SENTRY_ANDROID_ID || it.name == SENTRY_ANDROID_CORE_ID)
    }?.version

private fun DependencySet.isModuleAvailable(id: String): Boolean =
    any { it.group == SENTRY_GROUP && it.name == id }
