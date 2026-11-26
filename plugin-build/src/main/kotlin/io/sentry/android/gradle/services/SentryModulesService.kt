@file:Suppress("UnstableApiUsage") // Shared build services are incubating but available from 6.1

package io.sentry.android.gradle.services

import com.android.build.gradle.internal.utils.setDisallowChanges
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.EnabledInstrumentations
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.SentryModuleVersions
import io.sentry.android.gradle.util.SentryModuleVersions.Companion.key
import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

abstract class SentryModulesService :
  BuildService<SentryModulesService.Parameters>, OperationCompletionListener {

  @get:Synchronized @set:Synchronized var sentryModules: Map<ModuleIdentifier, SemVer> = emptyMap()

  @get:Synchronized
  @set:Synchronized
  var externalModules: Map<ModuleIdentifier, SemVer> = emptyMap()

  /** Which instrumentations apply, given the modules detected for the last resolved variant. */
  fun enabledInstrumentations(): EnabledInstrumentations =
    EnabledInstrumentations(
      SentryModuleVersions(sentryModules.entries.associate { (id, v) -> id.key() to v.toString() }),
      parameters.features.get(),
      parameters.logcatEnabled.get(),
      parameters.appStartEnabled.get(),
    )

  fun retrieveEnabledInstrumentationFeatures(project: Project): Provider<Set<String>> {
    val features = enabledInstrumentations().integrationNames.toMutableSet()

    if (parameters.sourceContextEnabled.getOrElse(false)) {
      features.add("SourceContext")
    }

    if (parameters.dexguardEnabled.getOrElse(false)) {
      features.add("DexGuard")
    }

    return project.provider { features }
  }

  companion object {
    fun register(
      project: Project,
      features: Provider<Set<InstrumentationFeature>>,
      logcatEnabled: Provider<Boolean>,
      sourceContextEnabled: Provider<Boolean>,
      dexguardEnabled: Provider<Boolean>,
      appStartEnabled: Provider<Boolean>,
    ): Provider<SentryModulesService> {
      return project.gradle.sharedServices.registerIfAbsent(
        getBuildServiceName(SentryModulesService::class.java),
        SentryModulesService::class.java,
      ) {
        it.parameters.features.setDisallowChanges(features)
        it.parameters.logcatEnabled.setDisallowChanges(logcatEnabled)
        it.parameters.sourceContextEnabled.setDisallowChanges(sourceContextEnabled)
        it.parameters.dexguardEnabled.setDisallowChanges(dexguardEnabled)
        it.parameters.appStartEnabled.setDisallowChanges(appStartEnabled)
      }
    }
  }

  override fun onFinish(event: FinishEvent?) = Unit // no-op

  interface Parameters : BuildServiceParameters {

    @get:Input val features: SetProperty<InstrumentationFeature>

    @get:Input val logcatEnabled: Property<Boolean>

    @get:Input val sourceContextEnabled: Property<Boolean>

    @get:Input val dexguardEnabled: Property<Boolean>

    @get:Input val appStartEnabled: Property<Boolean>
  }
}
