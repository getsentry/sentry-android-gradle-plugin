package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.util.contentHash
import io.sentry.android.gradle.withDummyComposeFile
import java.io.File
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

/**
 * Tests for verifying proper task ordering when Kotlin Compose compiler is enabled.
 *
 * With Compose, the final mapping file is written by `mergeReleaseComposeMapping` task, not by R8
 * directly. This test verifies that `generateSentryProguardUuid` generates the UUID from the final
 * merged mapping file, not from an intermediate state.
 */
class SentryPluginComposeTaskOrderingTest :
  BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

  // Kotlin 2.3.0 is needed for the Compose compiler plugin which generates the mergeComposeMapping
  // task
  override val additionalBuildClasspath: String =
    """
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0"
        classpath "org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.0"
        """
      .trimIndent()

  @Test
  fun `proguard UUID is generated from final merged mapping file`() {
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
              id "com.android.application"
              id "org.jetbrains.kotlin.android"
              id "org.jetbrains.kotlin.plugin.compose"
              id "io.sentry.android.gradle"
            }

            android {
              namespace 'com.example'
              buildTypes {
                release {
                  minifyEnabled = true
                }
              }
              buildFeatures {
                compose true
              }
              compileOptions {
                sourceCompatibility JavaVersion.VERSION_17
                targetCompatibility JavaVersion.VERSION_17
              }
            }

            kotlin {
              jvmToolchain(17)
            }

            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:2.3.0"
                implementation 'androidx.compose.ui:ui:1.7.0'
                implementation 'androidx.compose.foundation:foundation:1.7.0'
                implementation 'androidx.activity:activity-compose:1.9.0'
            }

            sentry {
              autoUploadProguardMapping = false
              includeProguardMapping = true
              autoInstallation {
                enabled = false
              }
              telemetry = false
            }
            """
        .trimIndent()
    )

    testProjectDir.withDummyComposeFile()

    val result =
      runner
        .appendArguments("app:assembleRelease")
        .appendArguments("--configuration-cache")
        .appendArguments("-Pandroid.builtInKotlin=false")
        .build()

    assertTrue("BUILD SUCCESSFUL" in result.output, "Build should succeed")

    // Read the final mapping file and calculate expected UUID
    val mappingFile = File(testProjectDir.root, "app/build/outputs/mapping/release/mapping.txt")
    assertTrue(mappingFile.exists(), "Mapping file should exist at ${mappingFile.absolutePath}")

    val expectedUuid = UUID.nameUUIDFromBytes(mappingFile.contentHash().toByteArray())

    // Read the generated UUID from the properties file
    val propertiesFile =
      File(
        testProjectDir.root,
        "app/build/intermediates/assets/release/injectSentryDebugMetaPropertiesIntoAssetsRelease/sentry-debug-meta.properties",
      )
    assertTrue(
      propertiesFile.exists(),
      "Properties file should exist at ${propertiesFile.absolutePath}",
    )

    val properties = Properties().apply { propertiesFile.inputStream().use { load(it) } }
    val uuidString = properties.getProperty("io.sentry.ProguardUuids")
    assertNotNull(uuidString, "Properties file should contain ProguardUuids")

    val actualUuid = UUID.fromString(uuidString)

    assertEquals(
      expectedUuid,
      actualUuid,
      "UUID should be generated from the final mapping file hash. " +
        "Expected: $expectedUuid (from mapping file), Actual: $actualUuid",
    )
  }
}
