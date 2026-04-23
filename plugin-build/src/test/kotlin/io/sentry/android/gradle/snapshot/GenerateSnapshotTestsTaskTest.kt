package io.sentry.android.gradle.snapshot

import io.sentry.android.gradle.parseMajorVersion
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenerateSnapshotTestsTaskTest {

  @get:Rule val tmpDir = TemporaryFolder()

  @Test
  fun `generates test file in correct package directory`() {
    val task = createTask()

    task.generate()

    val outputDir = task.outputDir.get().asFile
    val expectedFile = File(outputDir, "io/sentry/snapshot/ComposablePreviewSnapshotTest.kt")
    assertTrue(expectedFile.exists())
  }

  @Test
  fun `cleans output directory on re-run`() {
    val task = createTask()
    val outputDir = task.outputDir.get().asFile

    // Create a stale file in the output directory
    val staleDir = File(outputDir, "io/sentry/snapshot")
    staleDir.mkdirs()
    val staleFile = File(staleDir, "OldFile.kt")
    staleFile.writeText("old content")
    assertTrue(staleFile.exists())

    task.generate()

    assertFalse(staleFile.exists())
    val expectedFile = File(outputDir, "io/sentry/snapshot/ComposablePreviewSnapshotTest.kt")
    assertTrue(expectedFile.exists())
  }

  @Test
  fun `generated file contains correct package declaration`() {
    val content = generateAndRead()

    assertTrue(content.contains("package io.sentry.snapshot"))
  }

  @Test
  fun `generated file scans all packages`() {
    val content = generateAndRead()

    assertTrue(content.contains(".scanAllPackages()"))
    assertFalse(content.contains(".scanPackageTrees("))
  }

  @Test
  fun `generated file includes private previews when enabled`() {
    val content = generateAndRead(includePrivatePreviews = true)

    assertTrue(content.contains(".includePrivatePreviews()"))
  }

  @Test
  fun `generated file excludes private previews by default`() {
    val content = generateAndRead()

    assertFalse(content.contains(".includePrivatePreviews()"))
  }

  @Test
  fun `generated file contains parameterized test runner`() {
    val content = generateAndRead()

    assertTrue(content.contains("@RunWith(Parameterized::class)"))
    assertTrue(content.contains("class ComposablePreviewSnapshotTest"))
  }

  @Test
  fun `generated sidecar metadata uses short display name`() {
    val content = generateAndRead()

    assertTrue(
      content.contains(
        "\"display_name\" to screenshotId.removePrefix(preview.declaringClass + \".\")"
      )
    )
  }

  @Test
  fun `generated sidecar metadata uses full screenshotId for image file name`() {
    val content = generateAndRead()

    assertTrue(content.contains("\"image_file_name\" to screenshotId"))
  }

  @Test
  fun `generated file writes sidecar json to images directory`() {
    val content = generateAndRead()

    assertTrue(content.contains("val imagesDir = File(snapshotDir, \"images\")"))
    assertTrue(content.contains("File(imagesDir, \"\${sidecarName}.json\").writeText(json)"))
  }

  @Test
  fun `generated sidecar filename is lowercased to match Paparazzi image filenames`() {
    val content = generateAndRead()

    assertTrue(
      content.contains("screenshotId.lowercase(Locale.US).replace(\"\\\\s\".toRegex(), \"_\")")
    )
  }

  @Test
  fun `generated sidecar reads SentrySnapshot annotation via reflection`() {
    val content = generateAndRead()

    assertTrue(content.contains("Class.forName(preview.declaringClass)"))
    assertTrue(content.contains("\"io.sentry.snapshots.runtime.SentrySnapshot\""))
    assertTrue(content.contains("getDeclaredMethod(\"diffThreshold\")"))
  }

  @Test
  fun `generated sidecar emits diff_threshold only when non-default`() {
    val content = generateAndRead()

    assertTrue(
      content.contains(
        "if (diffThreshold != null && diffThreshold != 0f) metadata[\"diff_threshold\"] = diffThreshold"
      )
    )
  }

  @Test
  fun `parseMajorVersion extracts major from standard semver`() {
    assertEquals(1, parseMajorVersion("1.3.5"))
    assertEquals(2, parseMajorVersion("2.0.0-alpha01"))
  }

  @Test
  fun `parseMajorVersion extracts major from dynamic versions`() {
    assertEquals(1, parseMajorVersion("1.+"))
    assertEquals(2, parseMajorVersion("2.+"))
  }

  @Test
  fun `parseMajorVersion returns default for unparseable versions`() {
    assertEquals(2, parseMajorVersion("latest.release"))
    assertEquals(2, parseMajorVersion(null))
    assertEquals(2, parseMajorVersion("+"))
  }

  @Test
  fun `generated file uses HtmlReportWriter without maxPercentDifference for paparazzi 1`() {
    val content = generateAndRead(paparazziMajorVersion = 1)

    assertTrue(content.contains("HtmlReportWriter()"))
    assertFalse(content.contains("HtmlReportWriter(maxPercentDifference"))
  }

  @Test
  fun `generated file uses HtmlReportWriter with maxPercentDifference for paparazzi 2`() {
    val content = generateAndRead(paparazziMajorVersion = 2)

    assertTrue(content.contains("HtmlReportWriter(maxPercentDifference = tolerance)"))
    assertFalse(content.contains("HtmlReportWriter()"))
  }

  private fun generateAndRead(
    includePrivatePreviews: Boolean = false,
    paparazziMajorVersion: Int = 2,
  ): String {
    val task = createTask(includePrivatePreviews, paparazziMajorVersion)
    task.generate()
    val file =
      File(task.outputDir.get().asFile, "io/sentry/snapshot/ComposablePreviewSnapshotTest.kt")
    return file.readText()
  }

  private fun createTask(
    includePrivatePreviews: Boolean = false,
    paparazziMajorVersion: Int = 2,
  ): GenerateSnapshotTestsTask {
    val project = ProjectBuilder.builder().build()
    return project.tasks
      .register("testGenerateSnapshotTests", GenerateSnapshotTestsTask::class.java) { task ->
        task.includePrivatePreviews.set(includePrivatePreviews)
        task.paparazziMajorVersion.set(paparazziMajorVersion)
        task.outputDir.set(tmpDir.newFolder("output"))
      }
      .get()
  }
}
