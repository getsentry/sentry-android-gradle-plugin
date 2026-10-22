package io.sentry.android.gradle

import io.sentry.android.gradle.util.SentryModules
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.provider.Property
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector

internal fun Project.configureNdkBackend(backend: Property<String>) {
  backend.finalizeValueOnRead()
  val warned = AtomicBoolean()
  configurations.configureEach { configuration ->
    // defer substitution rules to avoid eager resolution during task graph construction
    configuration.incoming.beforeResolve {
      configuration.resolutionStrategy.dependencySubstitution { substitutions ->
        substitutions.all { dependency ->
          val selected = backend.get()
          if (selected != "inproc" && selected != "native") {
            throw GradleException(
              "sentry.ndkBackend must be 'inproc' or 'native', but was '$selected'."
            )
          }
          val requested = dependency.requested
          if (
            selected == "native" &&
              requested is ModuleComponentSelector &&
              requested.group == SentryModules.SENTRY_ANDROID_NDK.group &&
              requested.module == SentryModules.SENTRY_ANDROID_NDK.name
          ) {
            if (warned.compareAndSet(false, true)) {
              logger.warn(
                "[sentry] The 'native' backend on Android is experimental. " +
                  "It may have incomplete features or unexpected behavior."
              )
            }
            dependency.useTarget(
              // string notation rejects versionless dependencies managed by a BOM
              DefaultModuleComponentSelector.newSelector(
                SentryModules.SENTRY_ANDROID_NDK_OUTPROC,
                requested.versionConstraint,
              ),
              "sentry.ndkBackend selects the service-managed native crash backend",
            )
          }
        }
      }
    }
  }
}
