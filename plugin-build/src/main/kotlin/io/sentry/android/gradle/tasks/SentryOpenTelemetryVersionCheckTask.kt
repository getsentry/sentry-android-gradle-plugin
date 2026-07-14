package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

/**
 * Fails the build when the OpenTelemetry versions resolved on the runtime classpath were downgraded
 * below what the Sentry OpenTelemetry integration requires.
 *
 * Sentry's `sentry-opentelemetry-*` artifacts declare the exact OpenTelemetry versions they were
 * built and tested against. When another dependency management mechanism (most commonly
 * `io.spring.dependency-management` / the Spring Boot BOM) forces a lower OpenTelemetry version,
 * running against those downgraded versions can cause `ClassNotFoundException` /
 * `NoSuchMethodError` at runtime. We detect that downgrade here and fail fast with actionable
 * guidance instead of letting it blow up at runtime.
 */
@DisableCachingByDefault(because = "Only validates resolved dependency versions and has no outputs")
abstract class SentryOpenTelemetryVersionCheckTask : DefaultTask() {

  @get:Internal abstract val rootComponent: Property<ResolvedComponentResult>

  @get:Input abstract val docsUrl: Property<String>

  @get:Input abstract val springDependencyManagementApplied: Property<Boolean>

  @get:Input abstract val hasSentryOpenTelemetryDependency: Property<Boolean>

  @get:Input abstract val verifyEnabled: Property<Boolean>

  init {
    description =
      "Checks that the resolved OpenTelemetry versions are compatible with the Sentry " +
        "OpenTelemetry integration"
    group = "verification"
    // Reference task-owned properties only (not the extension/project) so the task stays
    // configuration-cache compatible.
    @Suppress("LeakingThis")
    onlyIf { verifyEnabled.get() && hasSentryOpenTelemetryDependency.get() }
  }

  @TaskAction
  fun action() {
    val downgrades = SentryOpenTelemetryVersionChecker.collectDowngrades(rootComponent.get())
    if (downgrades.isNotEmpty()) {
      throw GradleException(
        SentryOpenTelemetryVersionChecker.buildMessage(
          downgrades,
          docsUrl.get(),
          springDependencyManagementApplied.get(),
        )
      )
    }
  }

  companion object {
    private const val SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_ID = "io.spring.dependency-management"

    fun register(
      project: Project,
      extension: SentryPluginExtension,
      sentryTelemetryProvider: Provider<SentryTelemetryService>,
      configurationName: String,
      docsUrl: String,
    ): TaskProvider<SentryOpenTelemetryVersionCheckTask> {
      return project.tasks.register(
        "verifySentryOpenTelemetryVersions",
        SentryOpenTelemetryVersionCheckTask::class.java,
      ) { task ->
        val configuration = project.configurations.getByName(configurationName)
        task.rootComponent.set(configuration.incoming.resolutionResult.rootComponent)
        task.docsUrl.set(docsUrl)
        // Resolved lazily (at end of configuration) so it reflects the final plugin set regardless
        // of the order plugins are applied in the consumer's build.
        task.springDependencyManagementApplied.set(
          project.provider {
            project.pluginManager.hasPlugin(SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_ID)
          }
        )
        // Cheap declared-dependency check (no graph resolution) so we skip entirely for the common
        // case of projects that don't use Sentry OpenTelemetry at all.
        task.hasSentryOpenTelemetryDependency.set(
          project.provider {
            configuration.allDependencies.any {
              SentryOpenTelemetryVersionChecker.isSentryOpenTelemetryArtifact(it.group, it.name)
            }
          }
        )
        task.verifyEnabled.set(extension.verifyOpenTelemetryVersions)
        task.withSentryTelemetry(extension, sentryTelemetryProvider)
      }
    }
  }
}
