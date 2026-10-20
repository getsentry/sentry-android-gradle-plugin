package io.sentry.android.gradle

import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector

internal fun Project.configureNdkBackend(backend: Property<String>, logger: Logger = this.logger) {
  backend.finalizeValueOnRead()
  val warned = AtomicBoolean()
  configurations.configureEach { configuration ->
    configuration.resolutionStrategy.dependencySubstitution { substitutions ->
      substitutions.all { dependency ->
        val selected = backend.get()
        if (selected != "inproc" && selected != "native") {
          throw GradleException(
            "sentry.ndkBackend must be 'inproc' or 'native', but was '$selected'."
          )
        }
        if (selected == "native" && warned.compareAndSet(false, true)) {
          logger.warn(
            "[sentry] The 'native' backend on Android is experimental. " +
              "It may have incomplete features or unexpected behavior."
          )
        }
        val requested = dependency.requested
        if (
          selected == "native" &&
            requested is ModuleComponentSelector &&
            requested.group == "io.sentry" &&
            requested.module == "sentry-android-ndk"
        ) {
          dependency.useTarget(
            // string notation rejects versionless dependencies managed by a BOM
            DefaultModuleComponentSelector.newSelector(
              DefaultModuleIdentifier.newId(requested.group, "sentry-android-ndk-outproc"),
              requested.versionConstraint,
            ),
            "sentry.ndkBackend selects the service-managed native crash backend",
          )
        }
      }
    }
  }
}
