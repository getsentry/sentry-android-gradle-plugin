package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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
    val mismatches = collectDowngrades(rootComponent.get())
    if (mismatches.isNotEmpty()) {
      throw GradleException(
        buildMessage(mismatches, docsUrl.get(), springDependencyManagementApplied.get())
      )
    }
  }

  companion object {
    private const val SENTRY_GROUP = "io.sentry"
    private const val SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX = "sentry-opentelemetry-"
    private const val OTEL_GROUP = "io.opentelemetry"
    private const val SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_ID = "io.spring.dependency-management"

    internal data class VersionDowngrade(
      val module: String,
      val requested: String,
      val resolved: String,
    )

    /**
     * Walks the resolved dependency graph and returns, for every OpenTelemetry module that a
     * `sentry-opentelemetry-*` artifact depends on, the ones whose resolved version is lower than
     * the version Sentry requested.
     */
    internal fun collectDowngrades(root: ResolvedComponentResult): List<VersionDowngrade> {
      val mismatches = linkedMapOf<String, VersionDowngrade>()
      val visited = mutableSetOf<Any>()
      val stack = ArrayDeque<ResolvedComponentResult>()
      stack.addLast(root)
      while (stack.isNotEmpty()) {
        val component = stack.removeLast()
        if (!visited.add(component.id)) {
          continue
        }
        val fromSentryOpenTelemetry = component.isSentryOpenTelemetryArtifact()
        for (dependency in component.dependencies) {
          if (dependency !is ResolvedDependencyResult) {
            continue
          }
          if (fromSentryOpenTelemetry) {
            val requested = dependency.requested
            if (requested is ModuleComponentSelector && requested.isOpenTelemetry()) {
              val resolvedVersion = dependency.selected.moduleVersion?.version
              if (resolvedVersion != null && isDowngrade(requested.version, resolvedVersion)) {
                val module = "${requested.group}:${requested.module}"
                mismatches.putIfAbsent(
                  module,
                  VersionDowngrade(module, requested.version, resolvedVersion),
                )
              }
            }
          }
          stack.addLast(dependency.selected)
        }
      }
      return mismatches.values.toList()
    }

    private fun ResolvedComponentResult.isSentryOpenTelemetryArtifact(): Boolean {
      val moduleVersion = moduleVersion ?: return false
      return moduleVersion.group == SENTRY_GROUP &&
        moduleVersion.name.startsWith(SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX)
    }

    // Matches every OpenTelemetry group (io.opentelemetry, io.opentelemetry.instrumentation,
    // io.opentelemetry.semconv, ...) so any OTel module a sentry-opentelemetry-* artifact declares
    // is covered as that set grows. We only ever inspect edges declared by Sentry's own artifacts,
    // so this never flags unrelated OpenTelemetry usage.
    private fun ModuleComponentSelector.isOpenTelemetry(): Boolean =
      group == OTEL_GROUP || group.startsWith("$OTEL_GROUP.")

    private fun isDowngrade(requested: String, resolved: String): Boolean {
      // A blank requested version means it was declared without a concrete version (e.g. managed
      // elsewhere); there is nothing to compare against, so don't flag it.
      if (requested.isBlank() || requested == resolved) {
        return false
      }
      return try {
        SemVer.parse(resolved) < SemVer.parse(requested)
      } catch (e: IllegalArgumentException) {
        // Non-semver version strings can't be compared reliably; err on the side of not failing.
        false
      }
    }

    internal fun buildMessage(
      mismatches: List<VersionDowngrade>,
      docsUrl: String,
      springDependencyManagementApplied: Boolean,
    ): String {
      val details =
        mismatches.joinToString(separator = "\n") { mismatch ->
          "  - ${mismatch.module}: Sentry requires ${mismatch.requested} " +
            "but ${mismatch.resolved} was resolved"
        }
      val fix =
        if (springDependencyManagementApplied) {
          // io.spring.dependency-management enforces its managed versions via a resolution strategy
          // that overrides Gradle platform() constraints, so the BOM has to be imported through the
          // plugin's own API to win.
          """
          |To fix this, import the Sentry OpenTelemetry BOM through io.spring.dependency-management so
          |its versions win dependency resolution:
          |
          |  dependencyManagement {
          |    imports {
          |      mavenBom("io.sentry:sentry-opentelemetry-bom:<sentryVersion>")
          |    }
          |  }
          """
            .trimMargin()
        } else {
          """
          |To fix this, add the Sentry OpenTelemetry BOM as a platform dependency so its versions win
          |dependency resolution:
          |
          |  dependencies {
          |    implementation(platform("io.sentry:sentry-opentelemetry-bom:<sentryVersion>"))
          |  }
          """
            .trimMargin()
        }
      return """
        |Sentry detected that OpenTelemetry was downgraded below the version its integration requires.
        |
        |The Sentry OpenTelemetry integration was built against specific OpenTelemetry versions,
        |but the following were downgraded by another dependency management mechanism:
        |
        |$details
        |
        |Running with these downgraded OpenTelemetry versions can cause
        |ClassNotFoundException / NoSuchMethodError at runtime.
        |
        |$fix
        |
        |See $docsUrl for details.
        |
        |You can disable this check with:
        |
        |  sentry {
        |    autoInstallation.verifyOpenTelemetryVersions = false
        |  }
        """
        .trimMargin()
    }

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
              it.group == SENTRY_GROUP && it.name.startsWith(SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX)
            }
          }
        )
        task.verifyEnabled.set(extension.autoInstallation.verifyOpenTelemetryVersions)
        task.withSentryTelemetry(extension, sentryTelemetryProvider)
      }
    }
  }
}
