@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.CanMinifyCode
import com.android.build.api.variant.Variant
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.api.variant.impl.VariantImpl
import io.sentry.gradle.common.SentryVariant
import io.sentry.gradle.common.filterBuildConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider

data class AndroidVariant74(private val variant: Variant) : SentryVariant {
  override val name: String = variant.name
  override val flavorName: String? = variant.flavorName
  override val buildTypeName: String? = variant.buildType
  override val productFlavors: List<String> = variant.productFlavors.map { it.second }
  override val isMinifyEnabled: Boolean = (variant as? CanMinifyCode)?.isMinifyEnabled == true

  // TODO: replace this eventually (when targeting AGP 8.3.0) with
  // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-api/src/main/java/com/android/build/api/variant/Component.kt;l=103-104;bpv=1
  override val isDebuggable: Boolean = (variant as? ApplicationVariantImpl)?.debuggable == true

  // internal APIs are a bit dirty, but our plugin would need a lot of rework to make proper
  // dependencies via artifacts API.
  override val assembleProvider: TaskProvider<out Task>?
    get() = (variant as? VariantImpl<*>)?.taskContainer?.assembleTask

  override val installProvider: TaskProvider<out Task>?
    get() = (variant as? VariantImpl<*>)?.taskContainer?.installTask

  override fun mappingFileProvider(project: Project): Provider<FileCollection> =
    project.provider {
      project.files(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE))
    }

  override fun sources(
    project: Project,
    additionalSources: Provider<out Collection<Directory>>,
  ): Provider<out Collection<Directory>> {
    val javaProvider = variant.sources.java?.all
    val kotlinProvider = variant.sources.kotlin?.all

    // we cannot use .zip to combine the sources, because of possibly variation of this bug:
    // https://github.com/gradle/gradle/issues/23014, but using .map works just fine, so we just
    // call .get() inside the .map, and the providers will be lazily evaluated this way.
    return when {
      javaProvider == null && kotlinProvider == null -> additionalSources
      javaProvider == null ->
        kotlinProvider!!.map { kotlin ->
          (kotlin + additionalSources.get()).filterBuildConfig().toSet()
        }
      kotlinProvider == null ->
        javaProvider.map { java -> (java + additionalSources.get()).filterBuildConfig().toSet() }
      else ->
        javaProvider.map { java ->
          (java + kotlinProvider.get() + additionalSources.get()).filterBuildConfig().toSet()
        }
    }
  }

  fun <T : Task> assetsWiredWithDirectories(
    task: TaskProvider<T>,
    inputDir: (T) -> DirectoryProperty,
    outputDir: (T) -> DirectoryProperty,
  ) {
    variant.artifacts
      .use(task)
      .wiredWithDirectories(inputDir, outputDir)
      .toTransform(SingleArtifact.ASSETS)
  }
}

fun <T : Task> configureGeneratedSourcesFor74(
  variant: Variant,
  vararg tasks: Pair<TaskProvider<out T>, (T) -> DirectoryProperty>,
) {
  tasks.forEach { (task, output) ->
    variant.sources.assets?.addGeneratedSourceDirectory(task, output)
  }
}

fun <T : InstrumentationParameters> configureInstrumentationFor74(
  variant: Variant,
  classVisitorFactoryImplClass: Class<out AsmClassVisitorFactory<T>>,
  scope: InstrumentationScope,
  mode: FramesComputationMode,
  excludes: SetProperty<String>,
  instrumentationParamsConfig: (T) -> Unit,
) {
  variant.instrumentation.transformClassesWith(
    classVisitorFactoryImplClass,
    scope,
    instrumentationParamsConfig,
  )
  variant.instrumentation.setAsmFramesComputationMode(mode)
  variant.instrumentation.excludes.set(excludes)
}
