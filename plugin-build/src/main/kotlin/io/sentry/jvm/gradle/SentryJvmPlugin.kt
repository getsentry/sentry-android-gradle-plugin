package io.sentry.jvm.gradle

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.SentryTasksProvider
import io.sentry.android.gradle.autoinstall.installDependencies
import io.sentry.android.gradle.cliExecutableProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.sourcecontext.OutputPaths
import io.sentry.android.gradle.sourcecontext.SourceContext
import io.sentry.android.gradle.tasks.SentryGenerateDebugMetaPropertiesTask
import io.sentry.android.gradle.tasks.dependencies.SentryExternalDependenciesReportTaskFactory
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.util.SentryPluginUtils
import io.sentry.android.gradle.util.hookWithAssembleTasks
import io.sentry.android.gradle.util.info
import io.sentry.gradle.common.JavaVariant
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal

class SentryJvmPlugin
@Inject
constructor(private val buildEvents: BuildEventListenerRegistryInternal) : Plugin<Project> {

  /**
   * Since we're listening for the JavaBasePlugin, there may be multiple plugins inherting from it
   * applied to the same project, e.g. Spring Boot + Kotlin Jvm, hence we only want our plugin to be
   * configured only once.
   */
  private val configuredForJavaProject = AtomicBoolean(false)

  override fun apply(project: Project) {
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java, project)

    project.pluginManager.withPlugin("org.gradle.java") {
      if (configuredForJavaProject.getAndSet(true)) {
        SentryPlugin.logger.info { "The Sentry Gradle plugin was already configured" }
        return@withPlugin
      }

      val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)

      val sentryResDir = project.layout.buildDirectory.dir("generated${sep}sentry")

      val javaVariant = JavaVariant(project, javaExtension)
      val outputPaths = OutputPaths(project, "java")
      val cliExecutable = project.cliExecutableProvider()

      val extraProperties = project.extensions.getByName("ext") as ExtraPropertiesExtension

      val sentryOrgParameter =
        runCatching { extraProperties.get(SentryPlugin.SENTRY_ORG_PARAMETER).toString() }
          .getOrNull()
      val sentryProjectParameter =
        runCatching { extraProperties.get(SentryPlugin.SENTRY_PROJECT_PARAMETER).toString() }
          .getOrNull()

      val sentryTelemetryProvider = SentryTelemetryService.register(project)
      project.gradle.taskGraph.whenReady {
        sentryTelemetryProvider.get().start {
          SentryTelemetryService.createParameters(
            project,
            javaVariant,
            extension,
            cliExecutable,
            sentryOrgParameter,
            "JVM",
          )
        }
        buildEvents.onOperationCompletion(sentryTelemetryProvider)
      }

      val additionalSourcesProvider =
        project.provider {
          extension.additionalSourceDirsForSourceContext.getOrElse(emptySet()).map {
            project.layout.projectDirectory.dir(it)
          }
        }
      val sourceFiles = javaVariant.sources(project, additionalSourcesProvider)

      val sourceContextTasks =
        SourceContext.register(
          project,
          extension,
          sentryTelemetryProvider,
          javaVariant,
          outputPaths,
          sourceFiles,
          cliExecutable,
          sentryOrgParameter,
          sentryProjectParameter,
          "Java",
        )

      sourceContextTasks.uploadSourceBundleTask.hookWithAssembleTasks(project, javaVariant)

      javaExtension.sourceSets.getByName("main").resources { sourceSet ->
        sourceSet.srcDir(sentryResDir)
      }

      val generateDebugMetaPropertiesTask =
        SentryGenerateDebugMetaPropertiesTask.register(
          project,
          extension,
          sentryTelemetryProvider,
          listOf(sourceContextTasks.generateBundleIdTask),
          sentryResDir,
          "java",
        )

      val reportDependenciesTask =
        SentryExternalDependenciesReportTaskFactory.register(
          project = project,
          extension,
          sentryTelemetryProvider,
          configurationName = "runtimeClasspath",
          attributeValueJar = "jar",
          includeReport = extension.includeDependenciesReport,
          output = sentryResDir,
        )
      val resourcesTask =
        SentryPluginUtils.withLogging(project.logger, "processResources") {
          SentryTasksProvider.getProcessResourcesProvider(project)
        }
      resourcesTask?.configure { task ->
        task.dependsOn(reportDependenciesTask)
        task.dependsOn(generateDebugMetaPropertiesTask)
      }

      project.installDependencies(extension, false)
    }
  }

  companion object {
    internal val sep = File.separator
  }
}
