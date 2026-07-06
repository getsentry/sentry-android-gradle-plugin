package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.SemVer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ComponentSelectionCause
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

internal object SentryOpenTelemetryVersionChecker {
  private const val SENTRY_GROUP = "io.sentry"
  private const val SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX = "sentry-opentelemetry-"
  private const val OTEL_GROUP = "io.opentelemetry"

  data class VersionDowngrade(
    val module: String,
    val requested: String,
    val resolved: String,
    val requestedBy: String,
    val sentryBomVersion: String,
    val reason: String?,
  )

  fun collectDowngrades(root: ResolvedComponentResult): List<VersionDowngrade> {
    val downgrades = linkedMapOf<String, VersionDowngrade>()
    val visited = mutableSetOf<Any>()
    val stack = ArrayDeque<ResolvedComponentResult>()
    stack.addLast(root)
    while (stack.isNotEmpty()) {
      val component = stack.removeLast()
      if (!visited.add(component.id)) {
        continue
      }
      val sentryOpenTelemetryArtifact = component.sentryOpenTelemetryArtifact()
      for (dependency in component.dependencies) {
        if (dependency !is ResolvedDependencyResult) {
          continue
        }
        if (sentryOpenTelemetryArtifact != null) {
          val requested = dependency.requested
          if (requested is ModuleComponentSelector && requested.isOpenTelemetry()) {
            val resolvedVersion = dependency.selected.moduleVersion?.version
            if (resolvedVersion != null && isDowngrade(requested.version, resolvedVersion)) {
              val module = "${requested.group}:${requested.module}"
              downgrades.putIfAbsent(
                module,
                VersionDowngrade(
                  module = module,
                  requested = requested.version,
                  resolved = resolvedVersion,
                  requestedBy = sentryOpenTelemetryArtifact.toString(),
                  sentryBomVersion = sentryOpenTelemetryArtifact.version,
                  reason = dependency.selected.downgradeReason(),
                ),
              )
            }
          }
        }
        stack.addLast(dependency.selected)
      }
    }
    return downgrades.values.toList()
  }

  fun isSentryOpenTelemetryArtifact(group: String?, name: String): Boolean =
    group == SENTRY_GROUP && name.startsWith(SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX)

  fun buildMessage(
    downgrades: List<VersionDowngrade>,
    docsUrl: String,
    springDependencyManagementApplied: Boolean,
  ): String {
    val sentryBomVersion = downgrades.first().sentryBomVersion
    val details =
      downgrades.joinToString(separator = "\n") { downgrade ->
        buildString {
          append("  - ${downgrade.module}: Sentry requires ${downgrade.requested} ")
          append("but ${downgrade.resolved} was resolved")
          append("\n    Requested by: ${downgrade.requestedBy}")
          downgrade.reason?.let { append("\n    Gradle selection reason: $it") }
        }
      }
    val fix =
      if (springDependencyManagementApplied) {
        """
        |To fix this, import the Sentry OpenTelemetry BOM through io.spring.dependency-management so
        |its versions win dependency resolution:
        |
        |  dependencyManagement {
        |    imports {
        |      mavenBom("io.sentry:sentry-opentelemetry-bom:$sentryBomVersion")
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
        |    implementation(platform("io.sentry:sentry-opentelemetry-bom:$sentryBomVersion"))
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

  private fun ResolvedComponentResult.sentryOpenTelemetryArtifact(): ModuleVersionIdentifier? {
    val moduleVersion = moduleVersion ?: return null
    if (isSentryOpenTelemetryArtifact(moduleVersion.group, moduleVersion.name)) {
      return moduleVersion
    }
    return null
  }

  private fun ResolvedComponentResult.downgradeReason(): String? =
    selectionReason.descriptions
      .lastOrNull {
        it.cause == ComponentSelectionCause.CONSTRAINT ||
          it.cause == ComponentSelectionCause.FORCED ||
          it.cause == ComponentSelectionCause.CONFLICT_RESOLUTION ||
          it.cause == ComponentSelectionCause.SELECTED_BY_RULE
      }
      ?.description

  private fun ModuleComponentSelector.isOpenTelemetry(): Boolean =
    group == OTEL_GROUP || group.startsWith("$OTEL_GROUP.")

  private fun isDowngrade(requested: String, resolved: String): Boolean {
    if (requested.isBlank() || requested == resolved) {
      return false
    }
    return try {
      SemVer.parse(resolved) < SemVer.parse(requested)
    } catch (e: IllegalArgumentException) {
      false
    }
  }
}
