package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

/**
 * Verifies the Sentry snapshot upload flow when the Roborazzi plugin is applied. Roborazzi already
 * provides @Preview-driven Robolectric test generation; the Sentry plugin only configures it, pins
 * its outputDir, and wires `recordRoborazzi<Variant>` as a dependency of
 * `sentryUploadSnapshots<Variant>`.
 */
class SentryPluginRoborazziTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  override val additionalBuildClasspath: String =
    """
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21"
        classpath "io.github.takahirom.roborazzi:roborazzi-gradle-plugin:1.60.0"
        classpath "app.cash.paparazzi:paparazzi-gradle-plugin:1.3.5"
        """
      .trimIndent()

  @Test
  fun `wires sentryUploadSnapshotsDebug to depend on recordRoborazziDebug`() {
    appBuildFile.writeText(roborazziAppBuildScript())

    val build =
      runner.appendArguments(":app:sentryUploadSnapshotsDebug", "--dry-run", "--stacktrace").build()

    assertTrue(":app:sentryUploadSnapshotsDebug SKIPPED" in build.output)
    assertTrue(":app:recordRoborazziDebug SKIPPED" in build.output)
  }

  @Test
  fun `does not wire Paparazzi tasks when only Roborazzi is applied`() {
    appBuildFile.writeText(roborazziAppBuildScript())

    val build = runner.appendArguments(":app:tasks", "--all").build()

    assertFalse("recordPaparazzi" in build.output)
  }

  @Test
  fun `fails configuration when both Paparazzi and Roborazzi are applied`() {
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }

            // Paparazzi and Roborazzi are both already on the buildscript classpath via the
            // test fixture; apply them by id without re-declaring versions.
            apply plugin: "io.github.takahirom.roborazzi"
            apply plugin: "app.cash.paparazzi"

            android {
              namespace 'com.example'
            }

            sentry {
              autoUploadProguardMapping = false
              autoInstallation { enabled = false }
              telemetry = false
              snapshots { enabled = true }
            }
            """
        .trimIndent()
    )

    val result = runner.appendArguments(":app:tasks").buildAndFail()

    assertContains(
      result.output,
      "both app.cash.paparazzi and io.github.takahirom.roborazzi are applied",
    )
  }

  private fun roborazziAppBuildScript(): String =
    // language=Groovy
    """
        plugins {
          id "com.android.application"
          id "io.sentry.android.gradle"
          id "io.github.takahirom.roborazzi"
        }

        android {
          namespace 'com.example'
        }

        dependencies {
          testImplementation 'junit:junit:4.13.2'
          testImplementation 'org.robolectric:robolectric:4.14.1'
          testImplementation 'io.github.takahirom.roborazzi:roborazzi:1.60.0'
          testImplementation 'io.github.takahirom.roborazzi:roborazzi-compose:1.60.0'
          testImplementation 'io.github.takahirom.roborazzi:roborazzi-junit-rule:1.60.0'
        }

        sentry {
          autoUploadProguardMapping = false
          autoInstallation { enabled = false }
          telemetry = false
          snapshots {
            enabled = true
            previews {
              packageTrees = ['com.example']
            }
          }
        }
        """
      .trimIndent()
}
