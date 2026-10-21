package io.sentry.android.gradle

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.io.File
import java.util.jar.JarOutputStream
import kotlin.test.assertFailsWith
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class NdkBackendTest {
  @get:Rule val directory = TemporaryFolder()
  private val logger = mock<Logger>()

  @Test
  fun `defaults to inproc`() {
    val project = project()
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")

    assertThat(resolve(project)).containsExactly("sentry-android", "sentry-android-ndk", "inproc")
    verifyNoInteractions(logger)
  }

  @Test
  fun `native replaces transitive and direct ndk dependencies`() {
    val project = project()
    project.extensions.getByType(SentryPluginExtension::class.java).ndkBackend.set("native")
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")
    project.dependencies.add("runtime", "io.sentry:sentry-android-ndk:1.0")

    assertThat(resolve(project))
      .containsExactly("sentry-android", "sentry-android-ndk-outproc", "native")
  }

  @Test
  fun `native preserves the requested sdk version`() {
    val project = project()
    project.extensions.getByType(SentryPluginExtension::class.java).ndkBackend.set("native")
    publish("sentry-android-ndk-outproc", version = "2.0")
    project.dependencies.add("runtime", "io.sentry:sentry-android-ndk:2.0")

    assertThat(project.configurations.getByName("runtime").singleFile.name)
      .isEqualTo("sentry-android-ndk-outproc-2.0.jar")
  }

  @Test
  fun `rejects an unknown backend`() {
    val project = project()
    project.extensions.getByType(SentryPluginExtension::class.java).ndkBackend.set("other")
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")

    val error = assertFailsWith<ResolveException> { resolve(project) }
    val causes = generateSequence<Throwable>(error) { it.cause }.map { it.message }.joinToString()
    assertThat(causes).contains("sentry.ndkBackend must be 'inproc' or 'native'")
  }

  @Test
  fun `supports versionless dependencies with a BOM`() {
    val project = project()
    project.extensions.getByType(SentryPluginExtension::class.java).ndkBackend.set("native")
    val path = File(directory.root, "repository/io/sentry/sentry-bom/1.0").apply { mkdirs() }
    File(path, "sentry-bom-1.0.pom")
      .writeText(
        """
      <project><modelVersion>4.0.0</modelVersion>
      <groupId>io.sentry</groupId><artifactId>sentry-bom</artifactId><version>1.0</version>
      <packaging>pom</packaging><dependencyManagement><dependencies>
      <dependency><groupId>io.sentry</groupId><artifactId>sentry-android-ndk</artifactId><version>1.0</version></dependency>
      <dependency><groupId>io.sentry</groupId><artifactId>sentry-android-ndk-outproc</artifactId><version>1.0</version></dependency>
      </dependencies></dependencyManagement></project>
      """
          .trimIndent()
      )
    project.dependencies.add("runtime", project.dependencies.platform("io.sentry:sentry-bom:1.0"))
    project.dependencies.add("runtime", "io.sentry:sentry-android-ndk")

    assertThat(resolve(project)).containsExactly("sentry-android-ndk-outproc", "native")
  }

  @Test
  fun `warns once when native is selected`() {
    val project = project()
    project.extensions.getByType(SentryPluginExtension::class.java).ndkBackend.set("native")
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")
    val compile = project.configurations.create("compile")
    compile.extendsFrom(project.configurations.getByName("runtime"))
    compile.resolutionStrategy.disableDependencyVerification()

    resolve(project)
    compile.resolve()

    verify(logger)
      .warn(
        "[sentry] The 'native' backend on Android is experimental. " +
          "It may have incomplete features or unexpected behavior."
      )
  }

  @Test
  fun `keeps native dependencies unresolved when collecting task dependencies`() {
    val project = project()
    project.extensions.getByType(SentryPluginExtension::class.java).ndkBackend.set("native")
    project.dependencies.add("runtime", "io.sentry:sentry-android:1.0")
    val runtime = project.configurations.getByName("runtime")

    assertThat(runtime.buildDependencies.getDependencies(project.tasks.register("consume").get()))
      .isEmpty()
    assertThat(runtime.state).isEqualTo(Configuration.State.UNRESOLVED)
    verifyNoInteractions(logger)

    assertThat(resolve(project))
      .containsExactly("sentry-android", "sentry-android-ndk-outproc", "native")
  }

  private fun project(): Project {
    publish("inproc")
    publish("native")
    publish("sentry-android-ndk", "inproc")
    publish("sentry-android-ndk-outproc", "native")
    publish("sentry-android", "sentry-android-ndk")
    return ProjectBuilder.builder().withProjectDir(directory.newFolder("project")).build().also {
      it.pluginManager.apply("java-library")
      it.repositories.maven { repo -> repo.url = File(directory.root, "repository").toURI() }
      it.configurations.create("runtime").resolutionStrategy.disableDependencyVerification()
      val extension = it.extensions.create("sentry", SentryPluginExtension::class.java)
      it.configureNdkBackend(extension.ndkBackend, logger)
    }
  }

  private fun resolve(project: Project): List<String> =
    project.configurations.getByName("runtime").resolvedConfiguration.resolvedArtifacts.map {
      it.moduleVersion.id.name
    }

  private fun publish(name: String, dependency: String? = null, version: String = "1.0") {
    val path = File(directory.root, "repository/io/sentry/$name/$version").apply { mkdirs() }
    JarOutputStream(File(path, "$name-$version.jar").outputStream()).close()
    val dependencies =
      dependency
        ?.let {
          "<dependencies><dependency><groupId>io.sentry</groupId><artifactId>$it</artifactId>" +
            "<version>$version</version></dependency></dependencies>"
        }
        .orEmpty()
    File(path, "$name-$version.pom")
      .writeText(
        "<project><modelVersion>4.0.0</modelVersion><groupId>io.sentry</groupId>" +
          "<artifactId>$name</artifactId><version>$version</version>$dependencies</project>"
      )
  }
}
