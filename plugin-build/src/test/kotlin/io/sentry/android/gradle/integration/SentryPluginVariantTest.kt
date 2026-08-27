package io.sentry.android.gradle.integration

import com.google.common.truth.Truth.assertThat
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
  fun `disables tracing for variant via reverse dsl while preserving other sentry tasks`() {
    applyReverseVariantOverrides(
      variant = "fullDebug",
      tracingEnabled = false,
      runtimeOptimizationsEnabled = false,
    )

    val ignoredBuild = runner.appendArguments(":app:assembleFullDebug", "--dry-run").build()

    // instrumentation off for the overridden variant
    assertThat(ignoredBuild.output).doesNotContain(":app:transformFullDebugClassesWithAsm")
    assertThat(ignoredBuild.output).doesNotContain(":app:generateSentryBuildTimeOptionsFullDebug")
    // other plugin features still run (debug is not minified, so no mapping upload task)
    assertThat(ignoredBuild.output)
      .contains(":app:injectSentryDebugMetaPropertiesIntoAssetsFullDebug")

    val allowedBuild = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    assertThat(allowedBuild.output).contains(":app:transformFullReleaseClassesWithAsm")
    assertThat(allowedBuild.output).contains(":app:uploadSentryProguardMappingsFullRelease")
  }

  @Test
  fun `disables tracing for release variant via reverse dsl while preserving mapping upload`() {
    applyReverseVariantOverrides(variant = "fullRelease", tracingEnabled = false)

    val ignoredBuild = runner.appendArguments(":app:assembleFullRelease", "--dry-run").build()

    assertThat(ignoredBuild.output).doesNotContain(":app:transformFullReleaseClassesWithAsm")
    assertThat(ignoredBuild.output).contains(":app:uploadSentryProguardMappingsFullRelease")

    val allowedBuild = runner.appendArguments(":app:assembleDemoRelease", "--dry-run").build()

    assertThat(allowedBuild.output).contains(":app:transformDemoReleaseClassesWithAsm")
  }

  @Test
  fun `variant override only affects the named variant`() {
    applyReverseVariantOverrides(variant = "fullDebug", tracingEnabled = false)

    val overridden = runner.appendArguments(":app:assembleFullDebug", "--dry-run").build()
    assertThat(overridden.output).doesNotContain(":app:transformFullDebugClassesWithAsm")

    val sibling = runner.appendArguments(":app:assembleDemoDebug", "--dry-run").build()
    assertThat(sibling.output).contains(":app:transformDemoDebugClassesWithAsm")
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

  private fun applyReverseVariantOverrides(
    variant: String,
    tracingEnabled: Boolean,
    runtimeOptimizationsEnabled: Boolean = false,
  ) {
    appBuildFile.appendText(
      // language=Groovy
      """
                sentry {
                  autoUploadProguardMapping = false
                  tracingInstrumentation {
                    enabled = true
                  }
                  runtimeOptimizations {
                    enabled = true
                  }
                  variants {
                    $variant {
                      tracingInstrumentation {
                        enabled = $tracingEnabled
                      }
                      runtimeOptimizations {
                        enabled = $runtimeOptimizationsEnabled
                      }
                    }
                  }
                }
            """
        .trimIndent()
    )
  }
}
