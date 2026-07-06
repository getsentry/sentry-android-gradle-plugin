package io.sentry.android.gradle.integration

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryOpenTelemetryVersionCheckTest :
  BaseSentryNonAndroidPluginTest(GradleVersion.current().version) {

  private val verifyTask = ":app:verifySentryOpenTelemetryVersions"

  @Test
  fun `fails the build when OpenTelemetry is downgraded below what Sentry requires`() {
    // sentry-opentelemetry-agentless requires opentelemetry-sdk 1.63.0, but
    // io.spring.dependency-management forces it down to 1.62.0 (as Spring Boot's BOM would).
    writeSentryOpenTelemetryArtifact("2.0.0", "io.opentelemetry:opentelemetry-sdk:1.63.0")
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
                id "io.spring.dependency-management" version "1.1.7"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            dependencyManagement {
              dependencies {
                dependency 'io.opentelemetry:opentelemetry-sdk:1.62.0'
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").buildAndFail()

    assertEquals(TaskOutcome.FAILED, result.task(verifyTask)?.outcome)
    assertTrue {
      "OpenTelemetry was downgraded below the version its integration requires" in result.output
    }
    assertTrue { "io.opentelemetry:opentelemetry-sdk" in result.output }
    assertTrue { "Sentry requires 1.63.0 but 1.62.0 was resolved" in result.output }
    assertTrue { "Requested by: io.sentry:sentry-opentelemetry-agentless:2.0.0" in result.output }
    assertTrue { "Gradle selection reason:" in result.output }
    assertTrue { "verifyOpenTelemetryVersions = false" in result.output }
    assertTrue {
      "import the Sentry OpenTelemetry BOM through io.spring.dependency-management" in result.output
    }
    assertTrue {
      "mavenBom(\"io.sentry:sentry-opentelemetry-bom:<sentryVersion>\")" in result.output
    }
    assertFalse { "implementation platform" in result.output }
  }

  @Test
  fun `suggests a platform dependency when spring dependency-management is not applied`() {
    // No io.spring.dependency-management here; force the downgrade directly so the check triggers.
    writeSentryOpenTelemetryArtifact("2.0.0", "io.opentelemetry:opentelemetry-sdk:1.63.0")
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            configurations.all {
              resolutionStrategy {
                force 'io.opentelemetry:opentelemetry-sdk:1.62.0'
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").buildAndFail()

    assertEquals(TaskOutcome.FAILED, result.task(verifyTask)?.outcome)
    assertTrue { "Sentry requires 1.63.0 but 1.62.0 was resolved" in result.output }
    assertTrue { "Requested by: io.sentry:sentry-opentelemetry-agentless:2.0.0" in result.output }
    assertTrue { "Gradle selection reason: forced" in result.output }
    assertTrue {
      "implementation(platform(\"io.sentry:sentry-opentelemetry-bom:<sentryVersion>\"))" in
        result.output
    }
    assertFalse { "io.spring.dependency-management" in result.output }
  }

  @Test
  fun `passes when OpenTelemetry versions match what Sentry requires`() {
    writeSentryOpenTelemetryArtifact("2.0.0", "io.opentelemetry:opentelemetry-sdk:1.63.0")
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
                id "io.spring.dependency-management" version "1.1.7"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").build()

    assertEquals(TaskOutcome.SUCCESS, result.task(verifyTask)?.outcome)
  }

  @Test
  fun `respects configuration cache`() {
    writeSentryOpenTelemetryArtifact("2.0.0", "io.opentelemetry:opentelemetry-sdk:1.63.0")
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    runner.appendArguments("app:verifySentryOpenTelemetryVersions", "--configuration-cache")

    val result = runner.build()
    assertEquals(TaskOutcome.SUCCESS, result.task(verifyTask)?.outcome)
    assertTrue(result.output) { "Configuration cache entry stored." in result.output }

    val resultWithConfigurationCache = runner.build()
    assertEquals(TaskOutcome.SUCCESS, resultWithConfigurationCache.task(verifyTask)?.outcome)
    assertTrue(resultWithConfigurationCache.output) {
      "Configuration cache entry reused." in resultWithConfigurationCache.output
    }
  }

  @Test
  fun `is skipped when the project has no Sentry OpenTelemetry dependency`() {
    // A plain JVM project with the Sentry SDK but no sentry-opentelemetry-* dependency should not
    // run the check at all (no runtime classpath resolution, no failure).
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }

            repositories {
              mavenCentral()
            }

            dependencies {
              implementation 'io.sentry:sentry:8.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").build()

    assertEquals(TaskOutcome.SKIPPED, result.task(verifyTask)?.outcome)
  }

  @Test
  fun `does not fail when verifyOpenTelemetryVersions is disabled`() {
    writeSentryOpenTelemetryArtifact("2.0.0", "io.opentelemetry:opentelemetry-sdk:1.63.0")
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
                id "io.spring.dependency-management" version "1.1.7"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            dependencyManagement {
              dependencies {
                dependency 'io.opentelemetry:opentelemetry-sdk:1.62.0'
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.verifyOpenTelemetryVersions = false
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").build()

    assertEquals(TaskOutcome.SKIPPED, result.task(verifyTask)?.outcome)
  }

  @Test
  fun `a downgrade fails the regular build via the classes task`() {
    writeSentryOpenTelemetryArtifact("2.0.0", "io.opentelemetry:opentelemetry-sdk:1.63.0")
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
                id "io.spring.dependency-management" version "1.1.7"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            dependencyManagement {
              dependencies {
                dependency 'io.opentelemetry:opentelemetry-sdk:1.62.0'
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:classes").buildAndFail()

    assertEquals(TaskOutcome.FAILED, result.task(verifyTask)?.outcome)
    assertTrue {
      "OpenTelemetry was downgraded below the version its integration requires" in result.output
    }
  }

  @Test
  fun `detects downgrades of io_opentelemetry_semconv artifacts`() {
    // semconv lives in its own group (io.opentelemetry.semconv) and is versioned separately, so it
    // can be downgraded independently of the OpenTelemetry core.
    writeSentryOpenTelemetryArtifact(
      "2.0.0",
      "io.opentelemetry.semconv:opentelemetry-semconv:1.42.0",
    )
    appBuildFile.writeText(
      // language=Groovy
      """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
                id "io.spring.dependency-management" version "1.1.7"
            }

            repositories {
              maven { url file('../repo') }
              mavenCentral()
            }

            dependencyManagement {
              dependencies {
                dependency 'io.opentelemetry.semconv:opentelemetry-semconv:1.41.0'
              }
            }

            dependencies {
              implementation 'io.sentry:sentry-opentelemetry-agentless:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").buildAndFail()

    assertEquals(TaskOutcome.FAILED, result.task(verifyTask)?.outcome)
    assertTrue {
      "io.opentelemetry.semconv:opentelemetry-semconv: Sentry requires 1.42.0 but 1.41.0 was resolved" in
        result.output
    }
  }

  /**
   * Writes a minimal `io.sentry:sentry-opentelemetry-agentless` POM to the local file repo that
   * declares a hard dependency on the given OpenTelemetry module, so we can reproduce a downgrade
   * without depending on a real published Sentry artifact.
   */
  private fun writeSentryOpenTelemetryArtifact(version: String, requiredDependency: String) {
    val (group, name, requiredVersion) = requiredDependency.split(":")
    val pomFile =
      File(
        testProjectDir.root,
        "repo/io/sentry/sentry-opentelemetry-agentless/$version/" +
          "sentry-opentelemetry-agentless-$version.pom",
      )
    pomFile.parentFile.mkdirs()
    pomFile.writeText(
      """
          <project xmlns="http://maven.apache.org/POM/4.0.0"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <groupId>io.sentry</groupId>
            <artifactId>sentry-opentelemetry-agentless</artifactId>
            <version>$version</version>
            <packaging>jar</packaging>
            <dependencies>
              <dependency>
                <groupId>$group</groupId>
                <artifactId>$name</artifactId>
                <version>$requiredVersion</version>
              </dependency>
            </dependencies>
          </project>
          """
        .trimIndent()
    )
  }
}
