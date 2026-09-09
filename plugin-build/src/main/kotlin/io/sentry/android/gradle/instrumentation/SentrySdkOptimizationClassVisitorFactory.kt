package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.util.SentryModules
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.objectweb.asm.ClassVisitor

abstract class SentrySdkOptimizationClassVisitorFactory :
  AsmClassVisitorFactory<InstrumentationParameters.None> {

  override fun createClassVisitor(
    classContext: ClassContext,
    nextClassVisitor: ClassVisitor,
  ): ClassVisitor =
    when (classContext.currentClassData.className) {
      LOAD_CLASS_NAME ->
        LoadClassClassVisitor(instrumentationContext.apiVersion.get(), nextClassVisitor)
      MANIFEST_METADATA_READER_NAME ->
        ManifestMetadataClassVisitor(instrumentationContext.apiVersion.get(), nextClassVisitor)
      else -> nextClassVisitor
    }

  override fun isInstrumentable(classData: ClassData): Boolean =
    classData.className == LOAD_CLASS_NAME || classData.className == MANIFEST_METADATA_READER_NAME

  internal companion object {
    const val LOAD_CLASS_NAME = "io.sentry.util.LoadClass"
    const val MANIFEST_METADATA_READER_NAME = "io.sentry.android.core.ManifestMetadataReader"

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
