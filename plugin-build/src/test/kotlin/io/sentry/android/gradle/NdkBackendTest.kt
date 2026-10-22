package io.sentry.android.gradle

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NdkBackendTest {
  @get:Rule val directory = TemporaryFolder()

  @Test
  fun `defaults to inproc`() {
    val project = project()
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")

    assertThat(resolve(project))
      .containsExactly("sentry-android-1.0.jar", "sentry-android-ndk-1.0.jar")
  }

  @Test
  fun `native replaces transitive and direct ndk dependencies`() {
    val project = project("native")
    publish("sentry-android-ndk-outproc", version = "2.0")
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")
    project.dependencies.add("runtime", "io.sentry:sentry-android-ndk:2.0")
    val runtime = project.configurations.getByName("runtime")

    runtime.buildDependencies.getDependencies(null)
    assertThat(runtime.state).isEqualTo(Configuration.State.UNRESOLVED)

    assertThat(resolve(project))
      .containsExactly("sentry-android-1.0.jar", "sentry-android-ndk-outproc-2.0.jar")
  }

  @Test
  fun `supports versionless dependencies with a BOM`() {
    val project = project("native")
    publish(
      "sentry-bom",
      body =
        "<dependencyManagement>" +
          dependencies("sentry-android-ndk", "sentry-android-ndk-outproc") +
          "</dependencyManagement>",
    )
    project.dependencies.add("runtime", project.dependencies.platform("io.sentry:sentry-bom:1.0"))
    project.dependencies.add("runtime", "io.sentry:sentry-android-ndk")

    assertThat(resolve(project)).containsExactly("sentry-android-ndk-outproc-1.0.jar")
  }

  private fun project(backend: String? = null): Project {
    publish("sentry-android-ndk")
    publish("sentry-android-ndk-outproc")
    publish("sentry-android", body = dependencies("sentry-android-ndk"))
    return ProjectBuilder.builder().withProjectDir(directory.newFolder("project")).build().also {
      it.pluginManager.apply("java-library")
      it.repositories.maven { repo -> repo.url = File(directory.root, "repository").toURI() }
      it.configurations.create("runtime").resolutionStrategy.disableDependencyVerification()
      val extension = it.extensions.create("sentry", SentryPluginExtension::class.java)
      backend?.let(extension.ndkBackend::set)
      it.configureNdkBackend(extension.ndkBackend)
    }
  }

  private fun resolve(project: Project): List<String> =
    project.configurations.getByName("runtime").resolve().map { it.name }

  private fun publish(name: String, version: String = "1.0", body: String = "") {
    val path = File(directory.root, "repository/io/sentry/$name/$version").apply { mkdirs() }
    File(path, "$name-$version.jar").createNewFile()
    File(path, "$name-$version.pom")
      .writeText(
        "<project><modelVersion>4.0.0</modelVersion><groupId>io.sentry</groupId>" +
          "<artifactId>$name</artifactId><version>$version</version>$body</project>"
      )
  }

  private fun dependencies(vararg names: String): String =
    names.joinToString("", "<dependencies>", "</dependencies>") {
      "<dependency><groupId>io.sentry</groupId><artifactId>$it</artifactId>" +
        "<version>1.0</version></dependency>"
    }
}
