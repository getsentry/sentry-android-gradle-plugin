package io.sentry.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryCliProvider.getSentryCliPath
import io.sentry.android.gradle.autoinstall.installDependencies
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.util.AgpVersions
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.slf4j.LoggerFactory

@Suppress("UnstableApiUsage")
class SentryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_0) {
            throw StopExecutionException(
                """
                Using io.sentry.android.gradle:3+ with Android Gradle Plugin < 7 is not supported.
                Either upgrade the AGP version to 7+, or use an earlier version of the Sentry
                Android Gradle Plugin. For more information check our migration guide
                https://docs.sentry.io/platforms/android/migration/#migrating-from-iosentrysentry-android-gradle-plugin-2x-to-iosentrysentry-android-gradle-plugin-300
                """.trimIndent()
            )
        }

        val sentryTelemetryProvider: Provider<SentryTelemetryService> = project.gradle.sharedServices.registerIfAbsent(
            "sentry",
            SentryTelemetryService::class.java
        ) { spec ->
            // Provide some parameters
            spec.parameters.dsn.set("https://502f25099c204a2fbf4cb16edc5975d1@o447951.ingest.sentry.io/5428563")
        }

        val extension = project.extensions.create(
            "sentry",
            SentryPluginExtension::class.java,
            project
        )
        project.pluginManager.withPlugin("com.android.application") {
            val oldAGPExtension = project.extensions.getByType(AppExtension::class.java)
            val androidComponentsExt =
                project.extensions.getByType(AndroidComponentsExtension::class.java)
            val cliExecutable = getSentryCliPath(project)

            val extraProperties = project.extensions.getByName("ext")
                as ExtraPropertiesExtension

            val sentryOrgParameter = runCatching {
                extraProperties.get(SENTRY_ORG_PARAMETER).toString()
            }.getOrNull()
            val sentryProjectParameter = runCatching {
                extraProperties.get(SENTRY_PROJECT_PARAMETER).toString()
            }.getOrNull()

            // new API configuration
            androidComponentsExt.configure(
                project,
                extension,
                cliExecutable,
                sentryOrgParameter,
                sentryProjectParameter,
                sentryTelemetryProvider
            )

            // old API configuration
            oldAGPExtension.configure(
                project,
                extension,
                cliExecutable,
                sentryOrgParameter,
                sentryProjectParameter,
                sentryTelemetryProvider
            )

            project.installDependencies(extension, true)
        }
    }

    companion object {
        const val SENTRY_ORG_PARAMETER = "sentryOrg"
        const val SENTRY_PROJECT_PARAMETER = "sentryProject"
        internal const val SENTRY_SDK_VERSION = BuildConfig.SdkVersion

        internal val sep = File.separator

        // a single unified logger used by instrumentation
        internal val logger by lazy {
            LoggerFactory.getLogger(SentryPlugin::class.java)
        }
    }
}
