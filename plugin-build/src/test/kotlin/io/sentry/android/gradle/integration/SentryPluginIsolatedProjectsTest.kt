package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.GradleVersions
import io.sentry.android.gradle.util.SemVer
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Test

class SentryPluginIsolatedProjectsTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  @Test
  fun `plugin is compatible with isolated projects in a multi-module build`() {
    assumeThat(
      "Isolated Projects requires AGP 8.3.0+ and Gradle 8.0+",
      SemVer.parse(BuildConfig.AgpVersion) >= AgpVersions.VERSION_8_3_0 &&
        GradleVersions.CURRENT >= GradleVersions.VERSION_8_0,
      `is`(true),
    )

    writeIsolatedProjectsFixture()

    runner.appendArguments(":app:assembleDebug").appendArguments("--configuration-cache")

    val output = runner.build().output

    assertTrue(output) { "BUILD SUCCESSFUL" in output }
    // Sanity-check that isolated-projects was actually active during this build —
    // otherwise the test would silently degrade into a plain config-cache test.
    assertTrue(output) { "isolated projects" in output.lowercase() }
    assertFalse(
      "problems were found reporting" in output ||
        "Isolated projects violations" in output ||
        "cannot access '" in output,
      "Expected no isolated-projects violations, but got:\n$output",
    )
  }

  private fun writeIsolatedProjectsFixture() {
    // Replace the fixture written by BaseSentryPluginTest — its root build.gradle
    // uses allprojects {} and subprojects {}, which are fundamentally incompatible
    // with Gradle's isolated-projects feature. Instead, wire everything per-project
    // and add an Android library module that :app depends on, so we exercise
    // cross-project configuration (the interesting scenario for Isolated Projects).
    //
    // autoInstallation is intentionally left disabled: it walks the dependency
    // graph at configuration time and reaches into sibling projects' metadata
    // (Project.group), which violates Isolated Projects. Tracked in EME-1072.
    val pluginClasspath =
      PluginUnderTestMetadataReading.readImplementationClasspath()
        .joinToString(separator = ", ") { "\"$it\"" }
        .replace(File.separator, "/")

    File(testProjectDir.root, "settings.gradle")
      .writeText(
        // language=Groovy
        """
        include ':app', ':library'
        """
          .trimIndent()
      )

    File(testProjectDir.root, "gradle.properties")
      .writeText(
        """
        org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8
        org.gradle.daemon=false
        org.gradle.unsafe.isolated-projects=true
        android.useAndroidX=true
        android.overrideVersionCheck=true
        """
          .trimIndent()
      )

    File(testProjectDir.root, "build.gradle")
      .writeText(
        // language=Groovy
        """
        buildscript {
          repositories {
            google()
            gradlePluginPortal()
            mavenCentral()
          }
          dependencies {
            classpath 'com.android.tools.build:gradle:$androidGradlePluginVersion'
            classpath files($pluginClasspath)
          }
        }
        """
          .trimIndent()
      )

    File(testProjectDir.root, "app/build.gradle")
      .writeText(
        // language=Groovy
        """
        plugins {
          id 'com.android.application'
          id 'io.sentry.android.gradle'
        }

        repositories {
          google()
          mavenCentral()
        }

        android {
          namespace 'com.example'
          compileSdkVersion 35
          defaultConfig {
            applicationId 'com.example'
            minSdkVersion 21
          }
        }

        dependencies {
          implementation project(':library')
        }

        sentry {
          autoUploadProguardMapping = true
          autoInstallation.enabled = false
          telemetry = true
        }
        """
          .trimIndent()
      )

    val libraryDir = File(testProjectDir.root, "library").apply { mkdirs() }
    File(libraryDir, "build.gradle")
      .writeText(
        // language=Groovy
        """
        plugins {
          id 'com.android.library'
          id 'io.sentry.android.gradle'
        }

        repositories {
          google()
          mavenCentral()
        }

        android {
          namespace 'com.example.library'
          compileSdkVersion 35
          defaultConfig {
            minSdkVersion 21
          }
        }

        sentry {
          autoUploadProguardMapping = false
          autoInstallation.enabled = false
          telemetry = false
        }
        """
          .trimIndent()
      )
  }
}
