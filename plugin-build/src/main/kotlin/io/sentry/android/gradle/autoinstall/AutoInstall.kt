package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.autoinstall.compose.ComposeInstallStrategy
import io.sentry.android.gradle.autoinstall.fragment.FragmentInstallStrategy
import io.sentry.android.gradle.autoinstall.graphql.Graphql22InstallStrategy
import io.sentry.android.gradle.autoinstall.graphql.GraphqlInstallStrategy
import io.sentry.android.gradle.autoinstall.jdbc.JdbcInstallStrategy
import io.sentry.android.gradle.autoinstall.kotlin.KotlinExtensionsInstallStrategy
import io.sentry.android.gradle.autoinstall.log4j2.Log4j2InstallStrategy
import io.sentry.android.gradle.autoinstall.logback.LogbackInstallStrategy
import io.sentry.android.gradle.autoinstall.okhttp.AndroidOkHttpInstallStrategy
import io.sentry.android.gradle.autoinstall.okhttp.OkHttpInstallStrategy
import io.sentry.android.gradle.autoinstall.override.WarnOnOverrideStrategy
import io.sentry.android.gradle.autoinstall.quartz.QuartzInstallStrategy
import io.sentry.android.gradle.autoinstall.spring.Spring5InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.Spring6InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.Spring7InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.SpringBoot2InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.SpringBoot3InstallStrategy
import io.sentry.android.gradle.autoinstall.spring.SpringBoot4InstallStrategy
import io.sentry.android.gradle.autoinstall.sqlite.SQLiteInstallStrategy
import io.sentry.android.gradle.autoinstall.timber.TimberInstallStrategy
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.info
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet

internal const val SENTRY_GROUP = "io.sentry"

// Note: sentry-android-distribution is not included here because it requires variant-specific
// installation logic. Unlike other integrations that are installed globally when their
// dependencies are detected, distribution must be installed per-variant based on
// extension.distribution.enabledVariants. See
// AndroidComponentsConfig.configureDistributionPropertiesTask.
private val strategies =
  listOf(
    AndroidOkHttpInstallStrategy.Registrar,
    OkHttpInstallStrategy.Registrar,
    SQLiteInstallStrategy.Registrar,
    TimberInstallStrategy.Registrar,
    FragmentInstallStrategy.Registrar,
    ComposeInstallStrategy.Registrar,
    LogbackInstallStrategy.Registrar,
    Log4j2InstallStrategy.Registrar,
    JdbcInstallStrategy.Registrar,
    GraphqlInstallStrategy.Registrar,
    Graphql22InstallStrategy.Registrar,
    QuartzInstallStrategy.Registrar,
    KotlinExtensionsInstallStrategy.Registrar,
    WarnOnOverrideStrategy.Registrar,
  )

private val delayedStrategies =
  listOf(
    Spring5InstallStrategy.Registrar,
    Spring6InstallStrategy.Registrar,
    Spring7InstallStrategy.Registrar,
    SpringBoot2InstallStrategy.Registrar,
    SpringBoot3InstallStrategy.Registrar,
    SpringBoot4InstallStrategy.Registrar,
  )

fun Project.installDependencies(extension: SentryPluginExtension, isAndroid: Boolean) {
  configurations.named("implementation").configure { configuration ->
    configuration.withDependencies { dependencies ->
      project.dependencies.components { component ->
        delayedStrategies.forEach { it.register(component) }
      }

      // if autoInstallation is disabled, the autoInstallState will contain initial values
      // which all default to false, hence, the integrations won't be installed as well
      if (extension.autoInstallation.enabled.get()) {
        val sentryVersion = dependencies.findSentryVersion(isAndroid)
        with(AutoInstallState.getInstance(gradle)) {
          val sentryArtifactId =
            if (isAndroid) {
              SentryModules.SENTRY_ANDROID.name
            } else {
              SentryModules.SENTRY.name
            }
          this.sentryVersion =
            installSentrySdk(sentryVersion, dependencies, sentryArtifactId, extension)
          this.enabled = true
        }
      }
    }
  }
  project.dependencies.components { component -> strategies.forEach { it.register(component) } }
}

private fun Project.installSentrySdk(
  sentryVersion: String?,
  dependencies: DependencySet,
  sentryArtifactId: String,
  extension: SentryPluginExtension,
): String {
  return if (sentryVersion == null) {
    val userDefinedVersion = extension.autoInstallation.sentryVersion.get()
    val sentryAndroidDep =
      this.dependencies.create("$SENTRY_GROUP:$sentryArtifactId:$userDefinedVersion")
    dependencies.add(sentryAndroidDep)
    logger.info { "$sentryArtifactId was successfully installed with version: $userDefinedVersion" }
    userDefinedVersion
  } else {
    logger.info { "$sentryArtifactId won't be installed because it was already installed directly" }
    sentryVersion
  }
}

private fun DependencySet.findSentryVersion(isAndroid: Boolean): String? =
  if (isAndroid) {
    find {
        it.group == SENTRY_GROUP &&
          (it.name == SentryModules.SENTRY_ANDROID_CORE.name ||
            it.name == SentryModules.SENTRY_ANDROID.name ||
            it.name == SentryModules.SENTRY_BOM.name) &&
          it.version != null
      }
      ?.version
  } else {
    find {
        it.group == SENTRY_GROUP &&
          (it.name == SentryModules.SENTRY.name ||
            it.name == SentryModules.SENTRY_SPRING_BOOT2.name ||
            it.name == SentryModules.SENTRY_SPRING_BOOT3.name ||
            it.name == SentryModules.SENTRY_SPRING_BOOT4.name ||
            it.name == SentryModules.SENTRY_BOM.name ||
            it.name == SentryModules.SENTRY_OPENTELEMETRY_AGENTLESS.name ||
            it.name == SentryModules.SENTRY_OPENTELEMETRY_AGENTLESS_SPRING.name) &&
          it.version != null
      }
      ?.version
  }
