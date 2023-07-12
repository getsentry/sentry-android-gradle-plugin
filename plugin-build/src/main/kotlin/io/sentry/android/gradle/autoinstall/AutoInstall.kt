package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.autoinstall.compose.ComposeInstallStrategy
import io.sentry.android.gradle.autoinstall.compose.ComposeInstallStrategy.Registrar.SENTRY_COMPOSE_ID
import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy
import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy.Registrar.SENTRY_FRAGMENT_ID
import io.sentry.android.gradle.autoinstall.jdbc.JdbcInstallStrategy
import io.sentry.android.gradle.autoinstall.jdbc.JdbcInstallStrategy.Registrar.SENTRY_JDBC_ID
import io.sentry.android.gradle.autoinstall.kotlin.KotlinExtensionsInstallStrategy
import io.sentry.android.gradle.autoinstall.kotlin.KotlinExtensionsInstallStrategy.Registrar.SENTRY_KOTLIN_EXTENSIONS_ID
import io.sentry.android.gradle.autoinstall.log4j2.Log4j2InstallStrategy
import io.sentry.android.gradle.autoinstall.log4j2.Log4j2InstallStrategy.Registrar.SENTRY_LOG4J2_ID
import io.sentry.android.gradle.autoinstall.graphql.GraphqlInstallStrategy
import io.sentry.android.gradle.autoinstall.graphql.GraphqlInstallStrategy.Registrar.SENTRY_GRAPHQL_ID
import io.sentry.android.gradle.autoinstall.logback.LogbackInstallStrategy
import io.sentry.android.gradle.autoinstall.logback.LogbackInstallStrategy.Registrar.SENTRY_LOGBACK_ID
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy.Registrar.SENTRY_OKHTTP_ID
import io.sentry.android.gradle.autoinstall.spring.Spring5InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.Spring5InstallStrategy.Registrar.SENTRY_SPRING_5_ID
import io.sentry.android.gradle.autoinstall.spring.Spring6InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.Spring6InstallStrategy.Registrar.SENTRY_SPRING_6_ID
import io.sentry.android.gradle.autoinstall.spring.SpringBoot2InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.SpringBoot2InstallStrategy.Registrar.SENTRY_SPRING_BOOT_2_ID
import io.sentry.android.gradle.autoinstall.spring.SpringBoot3InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.SpringBoot3InstallStrategy.Registrar.SENTRY_SPRING_BOOT_3_ID
import io.sentry.android.gradle.autoinstall.sqlite.SQLiteInstallStrategy
import io.sentry.android.gradle.autoinstall.sqlite.SQLiteInstallStrategy.Registrar.SENTRY_SQLITE_ID
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy.Registrar.SENTRY_TIMBER_ID
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.info
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet

internal const val SENTRY_GROUP = "io.sentry"
private const val SENTRY_JAVA_ID = "sentry"
private const val SENTRY_ANDROID_ID = "sentry-android"
private const val SENTRY_ANDROID_CORE_ID = "sentry-android-core"

private val strategies = listOf(
    OkHttpInstallStrategy.Registrar,
    SQLiteInstallStrategy.Registrar,
    TimberInstallStrategy.Registrar,
    FragmentInstallStrategy.Registrar,
    ComposeInstallStrategy.Registrar,
    Spring5InstallStrategy.Registrar,
    Spring6InstallStrategy.Registrar,
    SpringBoot2InstallStrategy.Registrar,
    SpringBoot3InstallStrategy.Registrar,
    LogbackInstallStrategy.Registrar,
    Log4j2InstallStrategy.Registrar,
    JdbcInstallStrategy.Registrar,
    GraphqlInstallStrategy.Registrar,
    KotlinExtensionsInstallStrategy.Registrar
)

fun Project.installDependencies(extension: SentryPluginExtension, isAndroid: Boolean) {
    configurations.named("implementation").configure { configuration ->
        configuration.withDependencies { dependencies ->
            // if autoInstallation is disabled, the autoInstallState will contain initial values
            // which all default to false, hence, the integrations won't be installed as well
            if (extension.autoInstallation.enabled.get()) {
                val sentryVersion = dependencies.findSentryVersion(isAndroid)
                with(AutoInstallState.getInstance(gradle)) {
                    val sentryArtifactId = if (isAndroid) SENTRY_ANDROID_ID else SENTRY_JAVA_ID
                    this.sentryVersion = installSentrySdk(
                        sentryVersion,
                        dependencies,
                        sentryArtifactId,
                        extension
                    )

                    installOkHttp = !dependencies.isModuleAvailable(SENTRY_OKHTTP_ID)
                    installSqlite = !dependencies.isModuleAvailable(SENTRY_SQLITE_ID)
                    installTimber = !dependencies.isModuleAvailable(SENTRY_TIMBER_ID)
                    installFragment = !dependencies.isModuleAvailable(SENTRY_FRAGMENT_ID)
                    installCompose = !dependencies.isModuleAvailable(SENTRY_COMPOSE_ID)
                    installSpring = !(
                        dependencies.isModuleAvailable(SENTRY_SPRING_BOOT_2_ID) &&
                            dependencies.isModuleAvailable(SENTRY_SPRING_BOOT_3_ID) &&
                            dependencies.isModuleAvailable(SENTRY_SPRING_5_ID) &&
                            dependencies.isModuleAvailable(SENTRY_SPRING_6_ID)
                        )
                    installLogback = !dependencies.isModuleAvailable(SENTRY_LOGBACK_ID)
                    installLog4j2 = !dependencies.isModuleAvailable(SENTRY_LOG4J2_ID)
                    installJdbc = !dependencies.isModuleAvailable(SENTRY_JDBC_ID)
                    installGraphql = !dependencies.isModuleAvailable(SENTRY_GRAPHQL_ID)
                    installKotlinExtensions = !dependencies.isModuleAvailable(
                        SENTRY_KOTLIN_EXTENSIONS_ID
                    )
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
    sentryArtifactId: String,
    extension: SentryPluginExtension
): String {
    return if (sentryVersion == null) {
        val userDefinedVersion = extension.autoInstallation.sentryVersion.get()
        val sentryAndroidDep =
            this.dependencies.create("$SENTRY_GROUP:$sentryArtifactId:$userDefinedVersion")
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

private fun DependencySet.findSentryVersion(isAndroid: Boolean): String? =
    if (isAndroid) {
        find {
            it.group == SENTRY_GROUP &&
                (it.name == SENTRY_ANDROID_ID || it.name == SENTRY_ANDROID_CORE_ID)
        }?.version
    } else {
        find {
            it.group == SENTRY_GROUP &&
                (it.name == SENTRY_JAVA_ID)
        }?.version
    }

private fun DependencySet.isModuleAvailable(id: String): Boolean =
    any { it.group == SENTRY_GROUP && it.name == id }
