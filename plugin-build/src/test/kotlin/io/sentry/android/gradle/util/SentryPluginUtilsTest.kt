package io.sentry.android.gradle.util

import io.sentry.android.gradle.retrieveAndroidVariant
import io.sentry.android.gradle.testutil.createTestAndroidProject
import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import io.sentry.android.gradle.util.SentryPluginUtils.getAndDeleteFile
import io.sentry.android.gradle.util.SentryPluginUtils.isMinificationEnabled
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentryPluginUtilsTest(private val agpVersion: SemVer) {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `capitalizes string first letter uppercase`() {
    assertEquals("Kotlin", "kotlin".capitalizeUS())
  }

  @Test
  fun `capitalizes string does nothing on already capitalized string`() {
    assertEquals("Kotlin", "Kotlin".capitalizeUS())
  }

  @Test
  fun `capitalizes string returns empty on empty string`() {
    assertEquals("", "".capitalizeUS())
  }

  @Test
  fun `isMinificationEnabled returns false for debug builds`() {
    val (project, _) = createTestAndroidProject(forceEvaluate = !AgpVersions.isAGP74(agpVersion))
    val variant = project.retrieveAndroidVariant("debug")

    assertEquals(false, isMinificationEnabled(project, variant))
  }

  @Test
  fun `getAndDelete deletes the file`() {
    val (project, _) = createTestAndroidProject()
    val file = tempDir.newFile("temp-file.txt")

    assertTrue { file.exists() }

    getAndDeleteFile(project.layout.file(project.provider { file }))
    assertFalse { file.exists() }
  }

  companion object {
    @Parameterized.Parameters(name = "AGP {0}")
    @JvmStatic
    fun parameters() = listOf(AgpVersions.VERSION_7_4_0)
  }
}
