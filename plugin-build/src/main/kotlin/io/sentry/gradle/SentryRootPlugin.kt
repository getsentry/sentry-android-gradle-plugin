package io.sentry.gradle

import io.sentry.gradle.subplugin.RootPlugin
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.slf4j.LoggerFactory

internal abstract class SentryRootPlugin
@Inject
constructor(private val buildEvents: BuildEventListenerRegistryInternal) : Plugin<Project> {

  override fun apply(project: Project) {
    if (project == project.rootProject) {
      RootPlugin(project).apply(buildEvents)
    } else {
      throw StopExecutionException("io.sentry.gradle must be applied only to the root project.")
    }
  }

  companion object {
    // a single unified logger used by instrumentation
    internal val logger by lazy { LoggerFactory.getLogger(SentryRootPlugin::class.java) }
  }
}
