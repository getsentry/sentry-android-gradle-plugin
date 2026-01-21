@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.Variant
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Compatibility layer for AGP 8.3+.
 *
 * This class provides access to the `toListenTo` API, which was introduced in AGP 8.3. It allows
 * proper task wiring for artifacts without forcing their production.
 */
data class AndroidVariant83(
  private val variant: Variant,
  private val delegate: SentryVariant = AndroidVariant74(variant),
) : SentryVariant by delegate {

  override val isDebuggable: Boolean = variant.debuggable

  /**
   * Wires the [SentryGenerateProguardUuidTask] to listen to the obfuscation mapping file artifact.
   *
   * When minification runs, this ensures the UUID task executes after the mapping file is produced,
   * and receives the mapping file location via [SentryGenerateProguardUuidTask.mappingFile].
   *
   * When minification doesn't run, the UUID task won't be triggered via this wiring (it would only
   * run if explicitly depended upon by another task).
   */
  override fun wireMappingFileToUuidTask(
    project: Project,
    task: TaskProvider<out SentryGenerateProguardUuidTask>,
    variantName: String,
    dexguardEnabled: Boolean,
  ) {
    if (!dexguardEnabled) {
      variant.artifacts
        .use(task)
        .wiredWith(SentryGenerateProguardUuidTask::mappingFile)
        .toListenTo(SingleArtifact.OBFUSCATION_MAPPING_FILE)
    } else {
      // when dexguard is enabled we still want to go the old way, because AGP API does not apply
      // there
      delegate.wireMappingFileToUuidTask(project, task, variantName, true)
    }
  }
}
