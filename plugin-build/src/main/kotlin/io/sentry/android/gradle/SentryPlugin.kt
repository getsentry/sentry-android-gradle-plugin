package io.sentry.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryCliProvider.getSentryCliPath
import io.sentry.android.gradle.SentryTasksProvider.getProcessResourcesProvider
import io.sentry.android.gradle.autoinstall.installDependencies
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import io.sentry.android.gradle.util.info
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.StopExecutionException
import org.slf4j.LoggerFactory

@Suppress("UnstableApiUsage")
class SentryPlugin : Plugin<Project> {

    /**
     * Since we're listening for the JavaBasePlugin, there may be multiple plugins inherting from it
     * applied to the same project, e.g. Spring Boot + Kotlin Jvm, hence we only want our plugin to
     * be configured only once.
     */
    private val configuredForJavaProject = AtomicBoolean(false)

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

        val extension = project.extensions.create(
            "sentry",
            SentryPluginExtension::class.java,
            project
        )
        project.pluginManager.withPlugin("com.android.application") {
            val oldAGPExtension = project.extensions.getByType(AppExtension::class.java)
            val newAGPExtension =
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
            newAGPExtension.configure(
                project,
                extension,
                cliExecutable,
                sentryOrgParameter,
                sentryProjectParameter
            )

            // old API configuration
            oldAGPExtension.configure(
                project,
                extension,
                cliExecutable,
                sentryOrgParameter,
                sentryProjectParameter
            )

            project.installDependencies(extension)
        }

        project.pluginManager.withPlugin("org.gradle.java") {
            if (project.pluginManager.hasPlugin("com.android.application")) {
                // AGP also applies JavaBasePlugin, but since we have a separate setup for it,
                // we just bail here
                logger.info { "The Sentry Gradle plugin was already configured for AGP" }
                return@withPlugin
            }
            if (configuredForJavaProject.getAndSet(true)) {
                logger.info { "The Sentry Gradle plugin was already configured" }
                return@withPlugin
            }

            val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)

            val sentryResDir = project.layout.buildDirectory.dir("generated${sep}sentry")
            javaExtension.sourceSets.getByName("main").resources { sourceSet ->
                sourceSet.srcDir(sentryResDir)
            }

            val reportDependenciesTask = SentryExternalDependenciesReportTaskFactory.register(
                project = project,
                configurationName = "runtimeClasspath",
                attributeValueJar = "jar",
                includeReport = extension.includeDependenciesReport,
                output = sentryResDir
            )
            val resourcesTask = withLogging(project.logger, "processResources") {
                getProcessResourcesProvider(project)
            }
            resourcesTask?.configure { task -> task.dependsOn(reportDependenciesTask) }
        }
    }

    companion object {
        const val SENTRY_ORG_PARAMETER = "sentryOrg"
        const val SENTRY_PROJECT_PARAMETER = "sentryProject"
        internal const val SENTRY_SDK_VERSION = "6.11.0"

        internal val sep = File.separator

        // a single unified logger used by instrumentation
        internal val logger by lazy {
            LoggerFactory.getLogger(SentryPlugin::class.java)
        }
    }
}
