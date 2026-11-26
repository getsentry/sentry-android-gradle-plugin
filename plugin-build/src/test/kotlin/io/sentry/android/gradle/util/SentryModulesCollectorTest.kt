package io.sentry.android.gradle.util

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

class SentryModulesCollectorTest {

  class Fixture {

    private val configuration = mock<Configuration>()
    private val resolvableDependencies = mock<ResolvableDependencies>()
    private val resolutionResult = mock<ResolutionResult>()

    val configurationName: String = "debugRuntimeClasspath"
    val variantName: String = "debug"
    val logger = CapturingTestLogger()

    fun getSut(tmpDir: File, vararg dependencies: ResolvedComponentResult): Project {
      val fakeProject = ProjectBuilder.builder().withProjectDir(tmpDir).build()
      val root = component("org.example", "app", "unspecified", dependencies.toList())

      whenever(configuration.name).thenReturn(configurationName)
      whenever(configuration.incoming).thenReturn(resolvableDependencies)
      whenever(resolvableDependencies.resolutionResult).thenReturn(resolutionResult)
      whenever(resolutionResult.rootComponent).thenReturn(fakeProject.provider { root })

      val project = spy(fakeProject)
      whenever(project.logger).thenReturn(logger)
      project.configurations.add(configuration)
      return project
    }
  }

  @get:Rule val testProjectDir = TemporaryFolder()

  private val fixture = Fixture()

  @Test
  fun `configuration cannot be found - logs a warning and provides no versions`() {
    val project = fixture.getSut(testProjectDir.root)

    val versions = project.collectModules("releaseRuntimeClasspath", "release")

    assertThat(versions.get()).isEmpty()
    assertThat(fixture.logger.capturedMessage)
      .isEqualTo(
        "[sentry] Unable to find configuration releaseRuntimeClasspath for variant release."
      )
  }

  @Test
  fun `sentry is not in the dependencies - versions are empty`() {
    val project =
      fixture.getSut(testProjectDir.root, component("androidx.sqlite", "sqlite", "2.1.0"))

    val versions = project.collectModules(fixture.configurationName, fixture.variantName)

    assertThat(versions.get()).isEmpty()
  }

  @Test
  fun `sentry as a local dependency - the unparseable version is kept verbatim`() {
    val project =
      fixture.getSut(
        testProjectDir.root,
        component("io.sentry", "sentry-android-core", "unspecified"),
      )

    val versions = project.collectModules(fixture.configurationName, fixture.variantName)

    assertThat(versions.get()).containsExactly("io.sentry:sentry-android-core", "unspecified")
    assertThat(SentryModuleVersions(versions.get())[SentryModules.SENTRY_ANDROID_CORE])
      .isEqualTo(SemVer())
  }

  @Test
  fun `transitive sentry modules are collected`() {
    val core = component("io.sentry", "sentry-android-core", "5.5.0")
    val sentryAndroid = component("io.sentry", "sentry-android", "5.5.0", listOf(core))
    val okHttp = component("io.sentry", "sentry-android-okhttp", "6.0.0")

    val project = fixture.getSut(testProjectDir.root, sentryAndroid, okHttp)

    val versions = project.collectModules(fixture.configurationName, fixture.variantName)

    assertThat(versions.get())
      .containsExactly(
        "io.sentry:sentry-android",
        "5.5.0",
        "io.sentry:sentry-android-core",
        "5.5.0",
        "io.sentry:sentry-android-okhttp",
        "6.0.0",
      )
  }

  @Test
  fun `only the external modules the instrumentation branches on are collected`() {
    val project =
      fixture.getSut(
        testProjectDir.root,
        component("androidx.sqlite", "sqlite-framework", "2.3.0"),
        component("com.squareup.okhttp3", "okhttp", "4.9.0"),
        component("androidx.work", "work-runtime", "2.5.0"),
      )

    val versions = project.collectModules(fixture.configurationName, fixture.variantName)

    assertThat(versions.get())
      .containsExactly(
        "androidx.sqlite:sqlite-framework",
        "2.3.0",
        "com.squareup.okhttp3:okhttp",
        "4.9.0",
      )
  }

  @Test
  fun `a dependency cycle does not hang the traversal`() {
    val core = mock<ResolvedComponentResult>()
    val sentry = component("io.sentry", "sentry", "7.0.0", listOf(core))
    stubComponent(core, "io.sentry", "sentry-android-core", "7.0.0", listOf(sentry))

    val project = fixture.getSut(testProjectDir.root, sentry)

    val versions = project.collectModules(fixture.configurationName, fixture.variantName)

    assertThat(versions.get())
      .containsExactly("io.sentry:sentry", "7.0.0", "io.sentry:sentry-android-core", "7.0.0")
  }

  @Test
  fun `versions are sorted so the instrumentation input is stable`() {
    val project =
      fixture.getSut(
        testProjectDir.root,
        component("io.sentry", "sentry-android-okhttp", "6.0.0"),
        component("io.sentry", "sentry-android-core", "6.0.0"),
        component("io.sentry", "sentry", "6.0.0"),
      )

    val versions = project.collectModules(fixture.configurationName, fixture.variantName)

    assertThat(versions.get().keys)
      .containsExactly(
        "io.sentry:sentry",
        "io.sentry:sentry-android-core",
        "io.sentry:sentry-android-okhttp",
      )
      .inOrder()
  }
}

// Every mock is created before any stubbing starts: creating one inside a `whenever` argument
// trips Mockito's UnfinishedStubbingException.
private fun resolvedDependency(component: ResolvedComponentResult): ResolvedDependencyResult {
  val dependency = mock<ResolvedDependencyResult>()
  whenever(dependency.selected).thenReturn(component)
  return dependency
}

private fun component(
  group: String,
  name: String,
  version: String,
  dependencies: List<ResolvedComponentResult> = emptyList(),
): ResolvedComponentResult {
  val component = mock<ResolvedComponentResult>()
  stubComponent(component, group, name, version, dependencies)
  return component
}

private fun stubComponent(
  component: ResolvedComponentResult,
  group: String,
  name: String,
  version: String,
  dependencies: List<ResolvedComponentResult>,
) {
  val componentId = mock<ComponentIdentifier>()
  val moduleVersion = DefaultModuleVersionIdentifier.newId(group, name, version)
  val resolvedDependencies = dependencies.map(::resolvedDependency).toSet()

  whenever(component.id).thenReturn(componentId)
  whenever(component.moduleVersion).thenReturn(moduleVersion)
  whenever(component.dependencies).thenReturn(resolvedDependencies)
}
