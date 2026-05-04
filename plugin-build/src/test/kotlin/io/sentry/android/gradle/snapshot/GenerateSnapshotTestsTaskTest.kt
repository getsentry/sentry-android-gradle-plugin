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
    val task = createTask(packageTrees = listOf("com.example"))

    task.generate()

    val outputDir = task.outputDir.get().asFile
    val expectedFile = File(outputDir, "io/sentry/snapshot/ComposablePreviewSnapshotTest.kt")
    assertTrue(expectedFile.exists())
  }

  @Test
  fun `cleans output directory on re-run`() {
    val task = createTask(packageTrees = listOf("com.example"))
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
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("package io.sentry.snapshot"))
  }

  @Test
  fun `generated file scans configured package tree`() {
    val content = generateAndRead(packageTrees = listOf("com.example.app"))

    assertTrue(content.contains(".scanPackageTrees(\"com.example.app\")"))
  }

  @Test
  fun `generated file scans multiple package trees`() {
    val content =
      generateAndRead(packageTrees = listOf("com.example.feature1", "com.example.feature2"))

    assertTrue(
      content.contains(".scanPackageTrees(\"com.example.feature1\", \"com.example.feature2\")")
    )
  }

  @Test
  fun `generated file includes private previews when enabled`() {
    val content =
      generateAndRead(packageTrees = listOf("com.example"), includePrivatePreviews = true)

    assertTrue(content.contains(".includePrivatePreviews()"))
  }

  @Test
  fun `generated file excludes private previews by default`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertFalse(content.contains(".includePrivatePreviews()"))
  }

  @Test
  fun `generated file contains parameterized test runner`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("@RunWith(Parameterized::class)"))
    assertTrue(content.contains("class ComposablePreviewSnapshotTest"))
  }

  @Test
  fun `generated sidecar metadata uses short display name`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(
      content.contains(
        "\"display_name\" to screenshotId.removePrefix(preview.declaringClass + \".\")"
      )
    )
  }

  @Test
  fun `generated sidecar metadata uses full screenshotId for image file name`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("\"image_file_name\" to screenshotId"))
  }

  @Test
  fun `generated file writes sidecar json to images directory`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("val imagesDir = File(snapshotDir, \"images\")"))
    assertTrue(content.contains("File(imagesDir, \"\${sidecarName}.json\").writeText(json)"))
  }

  @Test
  fun `generated sidecar filename is lowercased to match Paparazzi image filenames`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(
      content.contains("screenshotId.lowercase(Locale.US).replace(\"\\\\s\".toRegex(), \"_\")")
    )
  }

  @Test
  fun `generated sidecar reads SentrySnapshot annotation via reflection`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("Class.forName(preview.declaringClass)"))
    assertTrue(content.contains("\"io.sentry.snapshots.runtime.SentrySnapshot\""))
    assertTrue(content.contains("getDeclaredMethod(\"diffThreshold\")"))
  }

  @Test
  fun `generated sidecar emits diff_threshold only when non-default`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(
      content.contains(
        "if (diffThreshold != null && diffThreshold != 0f) metadata[\"diff_threshold\"] = diffThreshold"
      )
    )
  }

  @Test
  fun `generated sidecar places preview location fields in context block`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("val context = linkedMapOf<String, Any>("))
    assertTrue(content.contains("\"image_file_name\" to screenshotId"))
    assertTrue(content.contains("\"class_name\" to preview.declaringClass"))
    assertTrue(content.contains("\"method_name\" to preview.methodName"))
    assertTrue(content.contains("metadata[\"context\"] = context"))
  }

  @Test
  fun `generated sidecar places appearance inputs in tags block`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("val tags = linkedMapOf<String, Any>()"))
    assertTrue(content.contains("if (info.name.isNotBlank()) tags[\"preview_name\"] = info.name"))
    assertTrue(content.contains("if (info.locale.isNotBlank()) tags[\"locale\"] = info.locale"))
    assertTrue(content.contains("if (info.device.isNotBlank()) tags[\"device\"] = info.device"))
    assertTrue(content.contains("if (info.fontScale != 1f) tags[\"font_scale\"] = info.fontScale"))
    assertTrue(content.contains("if (info.apiLevel != -1) tags[\"api_level\"] = info.apiLevel"))
    assertTrue(content.contains("if (info.widthDp > 0) tags[\"width_dp\"] = info.widthDp"))
    assertTrue(content.contains("if (info.heightDp > 0) tags[\"height_dp\"] = info.heightDp"))
    assertTrue(content.contains("if (info.showSystemUi) tags[\"show_system_ui\"] = true"))
    assertTrue(content.contains("if (info.showBackground) tags[\"show_background\"] = true"))
    assertTrue(content.contains("if (tags.isNotEmpty()) metadata[\"tags\"] = tags"))
  }

  @Test
  fun `generated sidecar places ui_mode in tags block`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(content.contains("when (info.uiMode and UI_MODE_NIGHT_MASK) {"))
    assertTrue(content.contains("UI_MODE_NIGHT_YES -> tags[\"ui_mode\"] = \"dark\""))
    assertTrue(content.contains("UI_MODE_NIGHT_NO -> tags[\"ui_mode\"] = \"light\""))
  }

  @Test
  fun `generated sidecar does not emit legacy night_mode field`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertFalse(content.contains("metadata[\"night_mode\"]"))
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
    val content = generateAndRead(packageTrees = listOf("com.example"), paparazziMajorVersion = 1)

    assertTrue(content.contains("HtmlReportWriter()"))
    assertFalse(content.contains("HtmlReportWriter(maxPercentDifference"))
  }

  @Test
  fun `generated file uses HtmlReportWriter with maxPercentDifference for paparazzi 2`() {
    val content = generateAndRead(packageTrees = listOf("com.example"), paparazziMajorVersion = 2)

    assertTrue(content.contains("HtmlReportWriter(maxPercentDifference = tolerance)"))
    assertFalse(content.contains("HtmlReportWriter()"))
  }

  private fun generateAndRead(
    packageTrees: List<String>,
    includePrivatePreviews: Boolean = false,
    paparazziMajorVersion: Int = 2,
  ): String {
    val task = createTask(packageTrees, includePrivatePreviews, paparazziMajorVersion)
    task.generate()
    val file =
      File(task.outputDir.get().asFile, "io/sentry/snapshot/ComposablePreviewSnapshotTest.kt")
    return file.readText()
  }

  private fun createTask(
    packageTrees: List<String>,
    includePrivatePreviews: Boolean = false,
    paparazziMajorVersion: Int = 2,
  ): GenerateSnapshotTestsTask {
    val project = ProjectBuilder.builder().build()
    return project.tasks
      .register("testGenerateSnapshotTests", GenerateSnapshotTestsTask::class.java) { task ->
        task.includePrivatePreviews.set(includePrivatePreviews)
        task.packageTrees.set(packageTrees)
        task.paparazziMajorVersion.set(paparazziMajorVersion)
        task.outputDir.set(tmpDir.newFolder("output"))
      }
      .get()
  }
}
