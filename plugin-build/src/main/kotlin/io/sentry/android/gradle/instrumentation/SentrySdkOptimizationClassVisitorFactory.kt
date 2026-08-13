package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.util.SentryModules
import java.util.Properties
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.objectweb.asm.ClassVisitor

abstract class SentrySdkOptimizationClassVisitorFactory :
  AsmClassVisitorFactory<SentrySdkOptimizationClassVisitorFactory.SdkOptimizationParameters> {

  interface SdkOptimizationParameters : InstrumentationParameters {
    /**
     * Properties file produced by
     * [io.sentry.android.gradle.tasks.dependencies.ResolveSdkClassAvailabilityTask]. Using a file
     * input keeps classpath resolution on the producing task and lets AGP invalidate
     * instrumentation when dependencies change.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val classAvailabilityFile: RegularFileProperty
  }

  override fun createClassVisitor(
    classContext: ClassContext,
    nextClassVisitor: ClassVisitor,
  ): ClassVisitor {
    return LoadClassClassVisitor(
      instrumentationContext.apiVersion.get(),
      nextClassVisitor,
      readClassAvailability(parameters.get().classAvailabilityFile),
    )
  }

  override fun isInstrumentable(classData: ClassData): Boolean =
    classData.className == LOAD_CLASS_NAME

  internal companion object {
    const val LOAD_CLASS_NAME = "io.sentry.util.LoadClass"

    val CLASS_MODULES: Map<String, Set<ModuleIdentifier>> =
      sortedMapOf(
        "androidx.compose.ui.node.Owner" to
          setOf(module("androidx.compose.ui", "ui"), module("androidx.compose.ui", "ui-android")),
        "androidx.core.view.ScrollingView" to setOf(module("androidx.core", "core")),
        "androidx.fragment.app.FragmentManager\$FragmentLifecycleCallbacks" to
          setOf(module("androidx.fragment", "fragment")),
        "androidx.lifecycle.Lifecycle" to
          setOf(
            module("androidx.lifecycle", "lifecycle-common"),
            module("androidx.lifecycle", "lifecycle-common-jvm"),
          ),
        "io.sentry.android.distribution.DistributionIntegration" to
          setOf(SentryModules.SENTRY_ANDROID_DISTRIBUTION),
        "io.sentry.android.fragment.FragmentLifecycleIntegration" to
          setOf(SentryModules.SENTRY_ANDROID_FRAGMENT),
        "io.sentry.android.replay.ReplayIntegration" to setOf(SentryModules.SENTRY_ANDROID_REPLAY),
        "io.sentry.android.timber.SentryTimberIntegration" to
          setOf(SentryModules.SENTRY_ANDROID_TIMBER),
        "io.sentry.compose.gestures.ComposeGestureTargetLocator" to
          setOf(SentryModules.SENTRY_ANDROID_COMPOSE, module("io.sentry", "sentry-compose")),
        "io.sentry.compose.viewhierarchy.ComposeViewHierarchyExporter" to
          setOf(SentryModules.SENTRY_ANDROID_COMPOSE, module("io.sentry", "sentry-compose")),
        "timber.log.Timber" to setOf(module("com.jakewharton.timber", "timber")),
      )

    private fun module(group: String, name: String): ModuleIdentifier =
      DefaultModuleIdentifier.newId(group, name)
  }
}

internal fun resolveClassAvailability(modules: Set<ModuleIdentifier>): Map<String, Boolean> =
  SentrySdkOptimizationClassVisitorFactory.CLASS_MODULES.mapValues { (_, owners) ->
    owners.any { it in modules }
  }

internal fun readClassAvailability(fileProperty: RegularFileProperty): Map<String, Boolean> {
  val file = fileProperty.orNull?.asFile ?: return emptyMap()
  if (!file.isFile || file.length() == 0L) {
    return emptyMap()
  }

  val properties = Properties()
  file.inputStream().use { properties.load(it) }
  return properties.entries.associate { (key, value) ->
    key.toString() to value.toString().toBooleanStrict()
  }
}
