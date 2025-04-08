package io.sentry.gradle.subplugin

import io.sentry.android.gradle.cliExecutableProvider
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.sourcecontext.RootOutputPaths
import io.sentry.android.gradle.sourcecontext.SourceContext
import io.sentry.android.gradle.sourcecontext.SourceContext.SourceContextTasks
import io.sentry.gradle.SENTRY_ORG_PARAMETER
import io.sentry.gradle.SENTRY_PROJECT_PARAMETER
import io.sentry.gradle.artifacts.Publisher.Companion.interProjectPublisher
import io.sentry.gradle.artifacts.Resolver
import io.sentry.gradle.artifacts.Resolver.Companion.interProjectResolver
import io.sentry.gradle.artifacts.SgpArtifacts
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal

internal class RootPlugin(private val project: Project) {
  init {
    check(project == project.rootProject) {
      "This plugin must only be applied to the root project. Was ${project.path}."
    }
  }

  private val bundleIdResolver = interProjectResolver(
    project = project,
    artifact = SgpArtifacts.Kind.BUNDLE_ID,
  )

  private val sourceRootsResolver = interProjectResolver(
    project = project,
    artifact = SgpArtifacts.Kind.SOURCE_ROOTS,
  )

  fun apply(buildEvents: BuildEventListenerRegistryInternal) =
    project.run {
      val sourceContextTasks = configureRootProject()

      subprojects { p ->
        val extraProperties = p.extensions.getByName("ext") as ExtraPropertiesExtension
        val sentryOrgParameter =
          runCatching { extraProperties.get(SENTRY_ORG_PARAMETER).toString() }.getOrNull()
        val sentryProjectParameter =
          runCatching { extraProperties.get(SENTRY_PROJECT_PARAMETER).toString() }.getOrNull()

        p.pluginManager.withPlugin("com.android.library") {
          val sentryExtension = SentryPluginExtension.of(p)
          AndroidLibSubplugin(p).apply()
        }
        p.pluginManager.withPlugin("com.android.application") {
          val sentryExtension = SentryPluginExtension.of(p)
          AndroidAppSubplugin(p, sentryExtension)
            .apply(buildEvents, sentryOrgParameter, sentryProjectParameter, sourceContextTasks)
        }

        // this has to be in afterEvaluate, else it might be too early to detect the deprecated
        // plugin
        p.afterEvaluate {
          p.pluginManager.withPlugin("com.android.application") {
            if (p.plugins.hasPlugin("io.sentry.android.gradle")) {
              throw StopExecutionException(
                "Using 'io.sentry.gradle' and 'io.sentry.android.gradle' plugins together is not supported."
              )
            }
          }
        }
      }
    }

  private fun Project.configureRootProject(): SourceContextTasks {
    val extras = extensions.getByName("ext") as ExtraPropertiesExtension
    val sentryExtension = SentryPluginExtension.of(this)
    val paths = RootOutputPaths(project)
    val cliExecutable = cliExecutableProvider()

    val sourceContextTasks =
      SourceContext.register(
        project,
        sentryExtension,
        null,
        null,
        paths,
        bundleIdResolver.artifactFilesProvider("properties"),
        sourceRootsResolver.artifactFilesProvider("txt"),
        cliExecutable,
        runCatching { extras.get(SENTRY_ORG_PARAMETER).toString() }.getOrNull(),
        runCatching { extras.get(SENTRY_PROJECT_PARAMETER).toString() }.getOrNull(),
        "",
      )

    // Add a dependency from the root project all projects (including itself).
    val bundleIdPublisher = interProjectPublisher(
      project = this,
      artifact = SgpArtifacts.Kind.BUNDLE_ID,
    )
    val sourceFilesPublisher = interProjectPublisher(
      project = this,
      artifact = SgpArtifacts.Kind.SOURCE_ROOTS,
    )
    subprojects.forEach { p ->
      dependencies.let { d ->
        d.add(bundleIdPublisher.declarableName, d.project(mapOf("path" to p.path)))
        d.add(sourceFilesPublisher.declarableName, d.project(mapOf("path" to p.path)))
      }
    }

    return sourceContextTasks
  }

  private val attributeKey = Attribute.of("artifactType", String::class.java)

  private fun Resolver<SgpArtifacts>.artifactFilesProvider(artifactType: String? = null): Provider<FileCollection> =
    internal.map { c ->
      c.incoming.artifactView {
        if (artifactType != null) {
          it.attributes.attribute(attributeKey, artifactType)
        }
        // Not all projects in the build will have SGP applied, meaning they won't have any artifact to consume.
        // Setting `lenient(true)` means we can still have a dependency on those projects, and not fail this task when
        // we find nothing there.
        it.lenient(true)
      }.artifacts.artifactFiles
    }
}
