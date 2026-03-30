package io.sentry.android.gradle.snapshot

import java.io.File
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
  fun `generated sidecar filename is lowercased`() {
    val content = generateAndRead(packageTrees = listOf("com.example"))

    assertTrue(
      content.contains("screenshotId.lowercase(Locale.US).replace(\"\\\\s\".toRegex(), \"_\")")
    )
  }

  private fun generateAndRead(
    packageTrees: List<String>,
    includePrivatePreviews: Boolean = false,
  ): String {
    val task = createTask(packageTrees, includePrivatePreviews)
    task.generate()
    val file =
      File(task.outputDir.get().asFile, "io/sentry/snapshot/ComposablePreviewSnapshotTest.kt")
    return file.readText()
  }

  private fun createTask(
    packageTrees: List<String>,
    includePrivatePreviews: Boolean = false,
  ): GenerateSnapshotTestsTask {
    val project = ProjectBuilder.builder().build()
    return project.tasks
      .register("testGenerateSnapshotTests", GenerateSnapshotTestsTask::class.java) { task ->
        task.includePrivatePreviews.set(includePrivatePreviews)
        task.packageTrees.set(packageTrees)
        task.outputDir.set(tmpDir.newFolder("output"))
      }
      .get()
  }
}
