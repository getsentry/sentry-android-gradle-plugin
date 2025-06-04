package io.sentry.android.gradle

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.gradle.SENTRY_ORG_PARAMETER
import io.sentry.gradle.SENTRY_PROJECT_PARAMETER
import io.sentry.gradle.subplugin.AndroidAppSubplugin
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.StopExecutionException
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.slf4j.LoggerFactory

@Suppress("UnstableApiUsage")
abstract class SentryPlugin
@Inject
constructor(private val buildEvents: BuildEventListenerRegistryInternal) : Plugin<Project> {

  override fun apply(project: Project) {
    project.logger.warn(
      """
      WARNING: Using 'io.sentry.android.gradle' is deprecated and this plugin will be removed in the future.
      Consider migrating to 'io.sentry.gradle' (root project gradle plugin) instead.
      """
        .trimIndent()
    )
    project.checkAgpApplied()
    project.checkAgpVersion()

    val extraProperties = project.extensions.getByName("ext") as ExtraPropertiesExtension
    val sentryOrgParameter =
      runCatching { extraProperties.get(SENTRY_ORG_PARAMETER).toString() }.getOrNull()
    val sentryProjectParameter =
      runCatching { extraProperties.get(SENTRY_PROJECT_PARAMETER).toString() }.getOrNull()

    val extension = SentryPluginExtension.of(project)
    project.pluginManager.withPlugin("com.android.application") {
      AndroidAppSubplugin(project, extension)
        .apply(buildEvents, sentryOrgParameter, sentryProjectParameter)
    }
  }

  private fun Project.checkAgpApplied() {
    if (!plugins.hasPlugin("com.android.application")) {
      logger.warn(
        """
        WARNING: Using 'io.sentry.android.gradle' is only supported for the app module.
        Please make sure that you apply the Sentry gradle plugin alongside 'com.android.application' on the _module_ level, and not on the root project level.
        https://docs.sentry.io/platforms/android/configuration/gradle/
        """
          .trimIndent()
      )
    }
  }

  private fun Project.checkAgpVersion() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_0) {
      throw StopExecutionException(
        """
        Using io.sentry.android.gradle:3+ with Android Gradle Plugin < 7 is not supported.
        Either upgrade the AGP version to 7+, or use an earlier version of the Sentry
        Android Gradle Plugin. For more information check our migration guide
        https://docs.sentry.io/platforms/android/migration/#migrating-from-iosentrysentry-android-gradle-plugin-2x-to-iosentrysentry-android-gradle-plugin-300
        """
          .trimIndent()
      )
    }
  }

  companion object {
    // a single unified logger used by instrumentation
    internal val logger by lazy { LoggerFactory.getLogger(SentryPlugin::class.java) }
  }
}
