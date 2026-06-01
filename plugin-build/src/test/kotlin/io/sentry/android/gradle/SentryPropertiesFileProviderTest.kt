package io.sentry.android.gradle

import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.testutil.createTestAndroidProject
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.SemVer
import java.io.File
import kotlin.test.assertEquals
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentryPropertiesFileProviderTest(private val agpVersion: SemVer) {

  @Test
  fun `getPropertiesFilePath finds file inside debug folder`() {
    val (project, _) = createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion))
    createTestFile(project.projectDir, "src/debug/sentry.properties")

    val variant = project.retrieveAndroidVariant("debug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside project folder`() {
    val (project, _) = createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion))
    createTestFile(project.projectDir, "sentry.properties")

    val variant = project.retrieveAndroidVariant("release")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside flavorName folder`() {
    val (project, _) =
      createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion)) {
        flavorDimensions("version")
        productFlavors.create("lite")
        productFlavors.create("full")
      }
    createTestFile(project.projectDir, "src/lite/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteDebug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside flavorName-buildType folder`() {
    val (project, _) =
      createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion)) {
        flavorDimensions("version")
        productFlavors.create("lite")
        productFlavors.create("full")
      }
    createTestFile(project.projectDir, "src/lite/debug/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteDebug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside buildType-flavorName folder`() {
    val (project, _) =
      createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion)) {
        flavorDimensions("version")
        productFlavors.create("lite")
        productFlavors.create("full")
      }
    createTestFile(project.projectDir, "src/debug/lite/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteDebug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath with multiple flavorDimensions finds file inside flavor folder`() {
    val (project, _) =
      createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion)) {
        flavorDimensions("version", "api")
        productFlavors.create("lite") { it.dimension("version") }
        productFlavors.create("api30") { it.dimension("api") }
      }
    createTestFile(project.projectDir, "src/liteApi30/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteApi30Debug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside other productFlavor folders`() {
    val (project, _) =
      createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion)) {
        flavorDimensions("version", "api")
        productFlavors.create("lite") { it.dimension("version") }
        productFlavors.create("api30") { it.dimension("api") }
      }
    createTestFile(project.projectDir, "src/api30/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteApi30Debug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside root project folder`() {
    val rootProject = ProjectBuilder.builder().build()
    val (project, _) =
      createTestAndroidProject(
        parent = rootProject,
        forceEvaluate = !AgpVersions.isAGP74(agpVersion),
      )
    createTestFile(rootProject.projectDir, "sentry.properties")

    val variant = project.retrieveAndroidVariant("release")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside root buildType folder`() {
    val rootProject = ProjectBuilder.builder().build()
    val (project, _) =
      createTestAndroidProject(
        parent = rootProject,
        forceEvaluate = !AgpVersions.isAGP74(agpVersion),
      )
    createTestFile(rootProject.projectDir, "src/debug/sentry.properties")

    val variant = project.retrieveAndroidVariant("debug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside root flavor folder`() {
    val rootProject = ProjectBuilder.builder().build()
    val (project, _) =
      createTestAndroidProject(
        parent = rootProject,
        forceEvaluate = !AgpVersions.isAGP74(agpVersion),
      ) {
        flavorDimensions("version")
        productFlavors.create("lite")
      }
    createTestFile(rootProject.projectDir, "src/lite/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteDebug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath finds file inside root flavor buildType folder`() {
    val rootProject = ProjectBuilder.builder().build()
    val (project, _) =
      createTestAndroidProject(
        parent = rootProject,
        forceEvaluate = !AgpVersions.isAGP74(agpVersion),
      ) {
        flavorDimensions("version")
        productFlavors.create("lite")
      }
    createTestFile(rootProject.projectDir, "src/lite/debug/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteDebug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath with null variant finds file inside project folder`() {
    val (project, _) = createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion))
    createTestFile(project.projectDir, "sentry.properties")

    assertEquals("42", File(getPropertiesFilePath(project)!!).readText())
  }

  @Test
  fun `getPropertiesFilePath with null variant skips buildType paths`() {
    val rootProject = ProjectBuilder.builder().build()
    val (project, _) =
      createTestAndroidProject(
        parent = rootProject,
        forceEvaluate = !AgpVersions.isAGP74(agpVersion),
      )
    // Only place the file under src/debug/ — with null variant this should not be found
    createTestFile(project.projectDir, "src/debug/sentry.properties")

    assertEquals(null, getPropertiesFilePath(project))
  }

  @Test
  fun `getPropertiesFilePath finds file inside root buildType flavor folder`() {
    val rootProject = ProjectBuilder.builder().build()
    val (project, _) =
      createTestAndroidProject(
        parent = rootProject,
        forceEvaluate = !AgpVersions.isAGP74(agpVersion),
      ) {
        flavorDimensions("version")
        productFlavors.create("lite")
      }
    createTestFile(rootProject.projectDir, "src/debug/lite/sentry.properties")

    val variant = project.retrieveAndroidVariant("liteDebug")

    assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
  }

  private fun createTestFile(parent: File, path: String) =
    File(parent, path).apply {
      parentFile.mkdirs()
      createNewFile()
      writeText("42")
    }

  companion object {
    @Parameterized.Parameters(name = "AGP {0}")
    @JvmStatic
    fun parameters() = listOf(AgpVersions.VERSION_7_4_0)
  }
}
