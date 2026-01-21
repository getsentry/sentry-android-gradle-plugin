package io.sentry.android.gradle.util

import io.sentry.android.gradle.SentryTasksProvider.getAssembleTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getInstallTaskProvider
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun TaskProvider<out Task>.hookWithAssembleTasks(project: Project, variant: SentryVariant) {
  // we need to wait for project evaluation to have all tasks available, otherwise the new
  // AndroidComponentsExtension is configured too early to look up for the tasks
  project.afterEvaluate {
    val bundleTask =
      withLogging(project.logger, "bundleTask") { getBundleTask(project, variant.name) }
    getAssembleTaskProvider(project, variant)?.configure { it.finalizedBy(this) }
    getInstallTaskProvider(project, variant)?.configure { it.finalizedBy(this) }
    // if its a bundle aab, assemble might not be executed, so we hook into bundle task
    bundleTask?.configure { it.finalizedBy(this) }
  }
}
