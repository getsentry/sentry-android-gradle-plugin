package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import org.gradle.util.GradleVersion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentryPluginVariantTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  override val additionalRootProjectConfig: String =
    // language=Groovy
    """
          flavorDimensions "version"
          productFlavors {
            create("demo") {
                applicationIdSuffix = ".demo"
            }
            create("full") {
                applicationIdSuffix = ".full"
            }
          }
        """
      .trimIndent()

  @Test
  fun `skips variant if set with ignoredVariants`() {
    applyIgnores(ignoredVariants = setOf("fullRelease"))

    val build = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    assertFalse(":app:uploadSentryProguardMappingsFullRelease" in build.output)
  }

  @Test
  fun `does not skip variant if not included in ignoredVariants`() {
    applyIgnores(ignoredVariants = setOf("demoRelease", "fullDebug", "demoDebug"))

    val build = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    assertTrue(":app:uploadSentryProguardMappingsFullRelease" in build.output)
  }

  @Test
  fun `skips buildType if set with ignoredBuildTypes`() {
    applyIgnores(ignoredBuildTypes = setOf("debug"))

    val build = runner.appendArguments(":app:assembleFullDebug", "--dry-run").build()

    assertFalse(":app:uploadSentryProguardMappingsFullDebug" in build.output)
    assertFalse(":app:uploadSentryProguardMappingsDemoDebug" in build.output)
  }

  @Test
  fun `does not skip buildType if not included in ignoredBuildTypes`() {
    applyIgnores(ignoredBuildTypes = setOf("debug"))

    val build = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    assertTrue(":app:uploadSentryProguardMappingsFullRelease" in build.output)
  }

  @Test
  fun `skips flavor if set with ignoredFlavors`() {
    applyIgnores(ignoredFlavors = setOf("full"))

    var build = runner.appendArguments(":app:assembleFullDebug", "--dry-run").build()

    assertFalse(":app:uploadSentryProguardMappingsFullDebug" in build.output)

    build = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    assertFalse(":app:uploadSentryProguardMappingsFullRelease" in build.output)
  }

  @Test
  fun `does not skip flavor if not included in ignoredFlavors`() {
    applyIgnores(ignoredFlavors = setOf("full"))

    val build = runner.appendArguments(":app:assembleDemoRelease", "--dry-run").build()

    assertTrue(":app:uploadSentryProguardMappingsDemoRelease" in build.output)
  }

  @Test
  fun `size analysis bypasses ignoredVariants when enabledVariants is set`() {
    appBuildFile.appendText(
      // language=Groovy
      """
                sentry {
                  autoUploadProguardMapping = false
                  ignoredVariants = ["fullRelease"]
                  sizeAnalysis {
                    enabled = true
                    enabledVariants = ["fullRelease"]
                  }
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """
        .trimIndent()
    )

    val build = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    // Size analysis upload task should be present despite variant being ignored
    assertTrue(":app:uploadSentryBundleFullRelease" in build.output)
    assertTrue(":app:uploadSentryApkFullRelease" in build.output)
    // Proguard mapping task should still be skipped (respects ignoredVariants)
    assertFalse(":app:uploadSentryProguardMappingsFullRelease" in build.output)
  }

  @Test
  fun `size analysis respects enabled flag even when variant is in enabledVariants`() {
    appBuildFile.appendText(
      // language=Groovy
      """
                sentry {
                  autoUploadProguardMapping = false
                  sizeAnalysis {
                    enabled = false
                    enabledVariants = ["fullRelease"]
                  }
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """
        .trimIndent()
    )

    val build = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    // Size analysis should be disabled
    assertFalse(":app:uploadSentryBundleFullRelease" in build.output)
    assertFalse(":app:uploadSentryApkFullRelease" in build.output)
  }

  @Test
  fun `size analysis runs on all variants when enabledVariants is empty and enabled is true`() {
    appBuildFile.appendText(
      // language=Groovy
      """
                sentry {
                  autoUploadProguardMapping = false
                  sizeAnalysis {
                    enabled = true
                  }
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """
        .trimIndent()
    )

    val buildRelease = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()
    val buildDebug = runner.appendArguments(":app:assembleFullDebug", "--dry-run").build()

    // Both variants should have size analysis tasks
    assertTrue(":app:uploadSentryBundleFullRelease" in buildRelease.output)
    assertTrue(":app:uploadSentryApkFullRelease" in buildRelease.output)
    assertTrue(":app:uploadSentryBundleFullDebug" in buildDebug.output)
    assertTrue(":app:uploadSentryApkFullDebug" in buildDebug.output)
  }

  @Test
  fun `size analysis only runs on specified variants when enabledVariants is not empty`() {
    appBuildFile.appendText(
      // language=Groovy
      """
                sentry {
                  autoUploadProguardMapping = false
                  sizeAnalysis {
                    enabled = true
                    enabledVariants = ["fullRelease"]
                  }
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """
        .trimIndent()
    )

    val buildRelease = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()
    val buildDebug = runner.appendArguments(":app:assembleFullDebug", "--dry-run").build()

    // Only fullRelease should have size analysis tasks
    assertTrue(":app:uploadSentryBundleFullRelease" in buildRelease.output)
    assertTrue(":app:uploadSentryApkFullRelease" in buildRelease.output)
    assertFalse(":app:uploadSentryBundleFullDebug" in buildDebug.output)
    assertFalse(":app:uploadSentryApkFullDebug" in buildDebug.output)
  }

  private fun applyIgnores(
    ignoredVariants: Set<String> = setOf(),
    ignoredBuildTypes: Set<String> = setOf(),
    ignoredFlavors: Set<String> = setOf(),
  ) {
    val variants = ignoredVariants.joinToString(",") { "\"$it\"" }
    val buildTypes = ignoredBuildTypes.joinToString(",") { "\"$it\"" }
    val flavors = ignoredFlavors.joinToString(",") { "\"$it\"" }
    appBuildFile.appendText(
      // language=Groovy
      """
                sentry {
                  autoUploadProguardMapping = false
                  ignoredVariants = [$variants]
                  ignoredBuildTypes = [$buildTypes]
                  ignoredFlavors = [$flavors]
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """
        .trimIndent()
    )
  }
}
