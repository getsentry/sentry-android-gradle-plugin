package io.sentry.android.gradle

import io.sentry.android.gradle.autoinstall.findSentryDependency
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.SentryModules
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector

internal fun Project.configureNdkBackend(extension: SentryPluginExtension) {
  val backend = extension.ndkBackend
  backend.finalizeValueOnRead()
  val warned = AtomicBoolean()
  fun isNative(): Boolean {
    val selected = backend.get()
    if (selected != "inproc" && selected != "native") {
      throw GradleException("sentry.ndkBackend must be 'inproc' or 'native', but was '$selected'.")
    }
    if (selected == "native" && warned.compareAndSet(false, true)) {
      logger.warn(
        "[sentry] The 'native' backend on Android is experimental. " +
          "It may have incomplete features or unexpected behavior."
      )
    }
    return selected == "native"
  }

  // the native backend builds on sentry-android-ndk and adds its crash service
  configurations.named("implementation").configure { configuration ->
    configuration.withDependencies { dependencies ->
      if (isNative()) {
        val version =
          dependencies.findSentryDependency(isAndroid = true).version
            ?: extension.autoInstallation.sentryVersion.get()
        val outproc = SentryModules.SENTRY_ANDROID_NDK_OUTPROC
        dependencies.add(project.dependencies.create("${outproc.group}:${outproc.name}:$version"))
      }
    }
  }

  configurations.configureEach { configuration ->
    // defer substitution rules to avoid eager resolution during task graph construction
    configuration.incoming.beforeResolve {
      configuration.resolutionStrategy.dependencySubstitution { substitutions ->
        substitutions.all { dependency ->
          val requested = dependency.requested
          if (
            requested is ModuleComponentSelector &&
              requested.group == SentryModules.SENTRY_NATIVE_NDK.group &&
              requested.module == SentryModules.SENTRY_NATIVE_NDK.name &&
              isNative()
          ) {
            dependency.useTarget(
              // string notation rejects versionless dependencies managed by a BOM
              DefaultModuleComponentSelector.newSelector(
                SentryModules.SENTRY_NATIVE_NDK_OUTPROC,
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
