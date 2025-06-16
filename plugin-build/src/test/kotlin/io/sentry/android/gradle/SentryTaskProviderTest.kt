package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getLintVitalAnalyzeProvider
import io.sentry.android.gradle.SentryTasksProvider.getLintVitalReportProvider
import io.sentry.android.gradle.SentryTasksProvider.getMinifyTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getProcessResourcesProvider
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryTaskProviderTest {

  @get:Rule val testProjectDir = TemporaryFolder()

  @Test
  fun `getTransformerTask returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getMinifyTask(project, "debug"))
  }

  @Test
  fun `getTransformerTask returns transform task for standalone Proguard with opt-in`() {
    val (project, task) =
      getTestProjectWithTask("transformClassesAndResourcesWithProguardTransformForDebug")

    assertEquals(task, getMinifyTask(project, "debug", dexguardEnabled = true)!!.get())
  }

  @Test
  fun `getTransformerTask returns null for standalone Proguard without opt-in`() {
    val (project, _) =
      getTestProjectWithTask("transformClassesAndResourcesWithProguardTransformForDebug")

    assertNull(getMinifyTask(project, "debug", dexguardEnabled = false))
  }

  @Test
  fun `getTransformerTask returns minify for R8`() {
    val (project, task) = getTestProjectWithTask("minifyDebugWithR8")

    assertEquals(task, getMinifyTask(project, "debug")!!.get())
  }

  @Test
  fun `getTransformerTask returns minify for embedded Proguard`() {
    val (project, task) = getTestProjectWithTask("minifyDebugWithProguard")

    assertEquals(task, getMinifyTask(project, "debug")!!.get())
  }

  @Test
  fun `getTransformerTask gives standalone Proguard priority with opt-in`() {
    val (project, _) = getTestProjectWithTask("minifyDebugWithR8")
    project.tasks.register("transformClassesAndResourcesWithProguardTransformForDebug")

    assertEquals(
      "transformClassesAndResourcesWithProguardTransformForDebug",
      getMinifyTask(project, "debug", dexguardEnabled = true)!!.name,
    )
  }

  @Test
  fun `getTransformerTask ignores standalone Proguard priority without opt-in`() {
    val (project, r8task) = getTestProjectWithTask("minifyDebugWithR8")
    project.tasks.register("transformClassesAndResourcesWithProguardTransformForDebug")

    assertEquals(r8task, getMinifyTask(project, "debug", dexguardEnabled = false)!!.get())
  }

  @Test
  fun `getPreBundleTask returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getPreBundleTask(project, "debug")?.get())
  }

  @Test
  fun `getPreBundleTask returns correct task`() {
    val (project, task) = getTestProjectWithTask("buildDebugPreBundle")

    assertEquals(task, getPreBundleTask(project, "debug")?.get())
  }

  @Test
  fun `getBundleTask returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getBundleTask(project, "debug")?.get())
  }

  @Test
  fun `getBundleTask returns correct task`() {
    val (project, task) = getTestProjectWithTask("bundleDebug")

    assertEquals(task, getBundleTask(project, "debug")?.get())
  }

  @Test
  fun `getPackageBundleTask returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getPackageBundleTask(project, "debug")?.get())
  }

  @Test
  fun `getPackageBundleTask returns package bundle task`() {
    val (project, task) = getTestProjectWithTask("packageDebugBundle")

    assertEquals(task, getPackageBundleTask(project, "debug")?.get())
  }

  @Test
  fun `getLintVitalAnalyze returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getLintVitalAnalyzeProvider(project, "debug")?.get())
  }

  @Test
  fun `getLintVitalAnalyze returns correct task`() {
    val (project, task) = getTestProjectWithTask("lintVitalAnalyzeDebug")

    assertEquals(task, getLintVitalAnalyzeProvider(project, "debug")?.get())
  }

  @Test
  fun `getLintVitalReport returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getLintVitalReportProvider(project, "debug")?.get())
  }

  @Test
  fun `getLintVitalReport returns correct task`() {
    val (project, task) = getTestProjectWithTask("lintVitalReportDebug")

    assertEquals(task, getLintVitalReportProvider(project, "debug")?.get())
  }

  @Test
  fun `getProcessResources returns null for missing task`() {
    val project = ProjectBuilder.builder().build()

    assertNull(getProcessResourcesProvider(project)?.get())
  }

  @Test
  fun `getProcessResources returns correct task`() {
    val (project, task) = getTestProjectWithTask("processResources")

    assertEquals(task, getProcessResourcesProvider(project)?.get())
  }

  private fun getAndroidExtFromProject(): Pair<Project, AppExtension> {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.android.application")
    val android =
      project.extensions.getByType(AppExtension::class.java).apply { compileSdkVersion(30) }

    // This forces the project to be evaluated
    project.getTasksByName("assembleDebug", false)
    return project to android
  }

  private fun getTestProjectWithTask(taskName: String): Pair<Project, Task> {
    val project = ProjectBuilder.builder().build()
    return project to project.tasks.register(taskName).get()
  }
}
