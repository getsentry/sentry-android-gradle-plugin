package io.sentry.android.gradle.sourcecontext

import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection

val SENTRY_ARTIFACT_ATTR: Attribute<String> = Attribute.of("io.sentry.artifact", String::class.java)

const val SENTRY_SOURCES_VALUE = "sentry-sources"

fun registerSentrySourceElements(project: Project) {
  val config =
    project.configurations.create("sentrySourceElements") {
      it.isCanBeConsumed = true
      it.isCanBeResolved = false
      it.attributes { attrs -> attrs.attribute(SENTRY_ARTIFACT_ATTR, SENTRY_SOURCES_VALUE) }
    }
  listOf("src/main/java", "src/main/kotlin").forEach { path ->
    val dir = project.file(path)
    if (dir.isDirectory) {
      config.outgoing.artifact(dir)
    }
  }
}

fun resolveDependencySources(project: Project, variantName: String): FileCollection {
  val sentrySourcesPath =
    project.configurations.create("sentrySourcesFor${variantName.capitalizeUS()}") {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.attributes { attrs -> attrs.attribute(SENTRY_ARTIFACT_ATTR, SENTRY_SOURCES_VALUE) }
    }

  val runtimeClasspath = project.configurations.getByName("${variantName}RuntimeClasspath")
  runtimeClasspath.extendsFrom
    .filter { !it.isCanBeResolved && !it.isCanBeConsumed }
    .forEach { sentrySourcesPath.extendsFrom(it) }

  return sentrySourcesPath.incoming.artifactView { view -> view.lenient(true) }.files
}
