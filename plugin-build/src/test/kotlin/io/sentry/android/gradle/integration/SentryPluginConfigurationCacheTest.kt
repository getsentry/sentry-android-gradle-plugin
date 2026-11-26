package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.SentryCliProvider
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.verifyDependenciesReportAndroid
import io.sentry.android.gradle.verifyIntegrationList
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Test

class SentryPluginConfigurationCacheTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  @Test
  fun `dependency collector task respects configuration cache`() {
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            repositories {
                flatDir {
                    dir('../libs')
                }
            }

            android {
              namespace 'com.example'
            }

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
              implementation project(':module') // multi-module project dependency
              implementation ':asm-9.2' // flat jar
            }

            sentry {
              autoUploadProguardMapping = false
              autoInstallation.enabled = false
              telemetry = false
            }
            """
        .trimIndent()
    )
    runner
      .appendArguments(":app:assembleDebug")
      .appendArguments("--configuration-cache")
      .appendArguments("--info")
      // AGP 9.+ adds a stdlib dependency which we don't want to have for deterministic output
      .appendArguments("-Pandroid.builtInKotlin=false")

    val output = runner.build().output
    val deps = verifyDependenciesReportAndroid(testProjectDir.root)
    assertEquals(
      """
            com.squareup.okhttp3:okhttp:3.14.9
            com.squareup.okio:okio:1.17.2
            """
        .trimIndent(),
      deps,
      "$deps\ndo not match expected value",
    )
    assertTrue { "Configuration cache entry stored." in output }

    val outputWithConfigCache = runner.build().output
    assertTrue { "Configuration cache entry reused." in outputWithConfigCache }
  }

  @Test
  fun `works well with configuration cache`() {
    // configuration cache doesn't seem to work well on older Gradle/AGP combinations
    // producing the following output:
    //
    // 0 problems were found storing the configuration cache.
    // Configuration cache entry discarded
    //
    // so we only run this test on supported versions
    assumeThat(
      "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
      SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
        GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
      `is`(true),
    )

    val runner =
      runner.withArguments("--configuration-cache", "--build-cache", ":app:assembleDebug")

    val run0 = runner.build()
    val run0Outcome = run0.task(":app:generateSentryBuildTimeOptionsDebug")?.outcome
    assertTrue(
      run0Outcome == TaskOutcome.SUCCESS || run0Outcome == TaskOutcome.FROM_CACHE,
      run0.output,
    )
    assertFalse(
      "Reusing configuration cache." in run0.output ||
        "Configuration cache entry reused." in run0.output,
      run0.output,
    )

    val run1 = runner.build()
    assertEquals(
      TaskOutcome.UP_TO_DATE,
      run1.task(":app:generateSentryBuildTimeOptionsDebug")?.outcome,
    )
    assertTrue(
      "Reusing configuration cache." in run1.output ||
        "Configuration cache entry reused." in run1.output,
      run1.output,
    )
  }

  @Test
  fun `sentry-cli is recovered when deleted during runs and configuration cache is active`() {
    // configuration cache doesn't seem to work well on older Gradle/AGP combinations
    // producing the following output:
    //
    // 0 problems were found storing the configuration cache.
    // Configuration cache entry discarded
    //
    // so we only run this test on supported versions
    assumeThat(
      "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
      SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
        GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
      `is`(true),
    )

    val runner =
      runner.withArguments("--configuration-cache", "--build-cache", ":app:assembleDebug")

    val run0 = runner.build()
    assertFalse(
      "Reusing configuration cache." in run0.output ||
        "Configuration cache entry reused." in run0.output,
      run0.output,
    )

    val buildDirProp =
      ProjectBuilder.builder().build().objects.directoryProperty().apply {
        set(File(runner.projectDir, "build"))
      }
    val cliPath = SentryCliProvider.getCliResourcesExtractionPath(buildDirProp).get().asFile

    // On Gradle >= 8, the whole build folder is wiped anyway
    if (cliPath.exists()) {
      cliPath.delete()
    }

    // then it should be recovered on the next run
    val run1 = runner.build()
    assertTrue(
      "Reusing configuration cache." in run1.output ||
        "Configuration cache entry reused." in run1.output,
      run1.output,
    )
    assertTrue(run1.output) { "BUILD SUCCESSFUL" in run1.output }
  }

  @Test
  fun `sentry-cli is recovered when clean is executed before assemble`() {
    assumeThat(
      "Sentry native symbols upload only supported when SENTRY_AUTH_TOKEN is present",
      System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty(),
      `is`(false),
    )
    // configuration cache doesn't seem to work well on older Gradle/AGP combinations
    // producing the following output:
    //
    // 0 problems were found storing the configuration cache.
    // Configuration cache entry discarded
    //
    // so we only run this test on supported versions
    assumeThat(
      "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
      SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
        GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
      `is`(true),
    )

    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
            }

            sentry {
              includeNativeSources = true
              uploadNativeSymbols = true
              includeProguardMapping = true
              autoUploadProguardMapping = true
              autoInstallation.enabled = false
              telemetry = false
            }
            """
        .trimIndent()
    )

    val runner =
      runner.withArguments(
        "--configuration-cache",
        "--build-cache",
        ":app:clean",
        ":app:assembleRelease",
        "--stacktrace",
      )

    val run = runner.build()
    assertTrue(run.output) { "BUILD SUCCESSFUL" in run.output }
  }

  @Test
  fun `native symbols upload task respects configuration cache`() {
    assumeThat(
      "Sentry native symbols upload only supported when SENTRY_AUTH_TOKEN is present",
      System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty(),
      `is`(false),
    )
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
            }

            sentry {
              includeNativeSources = true
              uploadNativeSymbols = true
              includeProguardMapping = false
              autoUploadProguardMapping = false
              autoInstallation.enabled = false
              telemetry = false
            }
            """
        .trimIndent()
    )
    runner.appendArguments(":app:assembleRelease").appendArguments("--configuration-cache")

    val output = runner.build().output
    assertTrue { "Configuration cache entry stored." in output }

    val outputWithConfigCache = runner.build().output
    assertTrue { "Configuration cache entry reused." in outputWithConfigCache }
  }

  @Test
  fun `generate integration list task respects configuration cache`() {
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
            }

            sentry {
              includeNativeSources = false
              uploadNativeSymbols = false
              includeProguardMapping = false
              autoUploadProguardMapping = false
              tracingInstrumentation.features = []
              telemetry = false
            }
            """
        .trimIndent()
    )
    runner.appendArguments(":app:assembleDebug").appendArguments("--configuration-cache")

    val firstBuild = runner.build().output
    assertTrue { "Configuration cache entry stored." in firstBuild }

    val integrationsFirst = verifyIntegrationList(testProjectDir.root, "debug", signed = false)
    assertEquals(
      listOf("AppStartInstrumentation", "LogcatInstrumentation"),
      integrationsFirst.sorted(),
    )

    // second build should reuse config cache and tasks should be up-to-date as nothing changed
    val secondBuild = runner.build()
    assertEquals(
      TaskOutcome.UP_TO_DATE,
      secondBuild.task(":app:debugSentryGenerateIntegrationListTask")?.outcome,
    )
    assertTrue { "Configuration cache entry reused." in secondBuild.output }

    val integrationsSecond = verifyIntegrationList(testProjectDir.root, "debug", signed = false)
    assertEquals(
      listOf("AppStartInstrumentation", "LogcatInstrumentation"),
      integrationsSecond.sorted(),
    )
  }

  @Test
  fun `instrumentation is applied when configuration cache is reused`() {
    assumeThat(
      "We only support configuration cache from AGP 7.4.0 and Gradle 8.0.0 onwards",
      SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_7_4_0 &&
        GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
      `is`(true),
    )
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
            }

            dependencies {
              implementation 'io.sentry:sentry-android-core:6.30.0'
            }

            sentry {
              autoUploadProguardMapping = false
              autoInstallation.enabled = false
              telemetry = false
              tracingInstrumentation.features = [
                io.sentry.android.gradle.extensions.InstrumentationFeature.FILE_IO
              ]
            }
            """
        .trimIndent()
    )
    val sourceFile =
      File(testProjectDir.root, "app/src/main/java/com/example/FileReader.java").apply {
        parentFile.mkdirs()
      }
    fun writeSource(extraMethod: String = "") =
      sourceFile.writeText(
        // language=Java
        """
          package com.example;

          import java.io.File;
          import java.io.FileInputStream;
          import java.io.IOException;

          public class FileReader {
            public int read(File file) throws IOException {
              try (FileInputStream stream = new FileInputStream(file)) {
                return stream.read();
              }
            }
            $extraMethod
          }
          """
          .trimIndent()
      )

    writeSource()
    runner.appendArguments(":app:assembleDebug").appendArguments("--configuration-cache")

    val firstBuild = runner.build()
    assertTrue("Configuration cache entry stored." in firstBuild.output, firstBuild.output)
    assertTrue(
      instrumentedClassBytes("FileReader.class").contains(SENTRY_FILE_INPUT_STREAM_FACTORY),
      "FileReader should be instrumented when the configuration cache entry is stored",
    )

    // A source change does not invalidate the configuration cache, but re-runs the ASM transform
    writeSource(extraMethod = "public void unused() {}")

    val secondBuild = runner.build()
    assertTrue("Configuration cache entry reused." in secondBuild.output, secondBuild.output)
    assertEquals(
      TaskOutcome.SUCCESS,
      secondBuild.task(":app:transformDebugClassesWithAsm")?.outcome,
      secondBuild.output,
    )
    assertTrue(
      instrumentedClassBytes("FileReader.class").contains(SENTRY_FILE_INPUT_STREAM_FACTORY),
      "FileReader should be instrumented when the configuration cache entry is reused",
    )
  }

  private fun instrumentedClassBytes(className: String): String {
    val classFile =
      File(testProjectDir.root, "app/build/intermediates/classes/debug").walkTopDown().single {
        it.name == className && "transformDebugClassesWithAsm" in it.path
      }
    return String(classFile.readBytes(), Charsets.ISO_8859_1)
  }

  companion object {
    private const val SENTRY_FILE_INPUT_STREAM_FACTORY =
      "io/sentry/instrumentation/file/SentryFileInputStream\$Factory"
  }
}
