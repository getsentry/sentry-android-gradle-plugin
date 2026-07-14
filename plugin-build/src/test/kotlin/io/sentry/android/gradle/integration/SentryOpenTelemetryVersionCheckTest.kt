package io.sentry.android.gradle.integration

import com.google.common.truth.Truth.assertThat
import java.io.File
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

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output)
      .contains("OpenTelemetry was downgraded below the version its integration requires")
    assertThat(result.output).contains("io.opentelemetry:opentelemetry-sdk")
    assertThat(result.output).contains("Sentry requires 1.63.0 but 1.62.0 was resolved")
    assertThat(result.output)
      .contains("Requested by: io.sentry:sentry-opentelemetry-agentless:2.0.0")
    assertThat(result.output).contains("Gradle selection reason:")
    assertThat(result.output).contains("verifyOpenTelemetryVersions = false")
    assertThat(result.output)
      .contains("import the Sentry OpenTelemetry BOM through io.spring.dependency-management")
    assertThat(result.output).contains("mavenBom(\"io.sentry:sentry-opentelemetry-bom:2.0.0\")")
    assertThat(result.output).doesNotContain("<sentryVersion>")
    assertThat(result.output).doesNotContain("implementation platform")
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

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output).contains("Sentry requires 1.63.0 but 1.62.0 was resolved")
    assertThat(result.output)
      .contains("Requested by: io.sentry:sentry-opentelemetry-agentless:2.0.0")
    assertThat(result.output).contains("Gradle selection reason: forced")
    assertThat(result.output)
      .contains("implementation(platform(\"io.sentry:sentry-opentelemetry-bom:2.0.0\"))")
    assertThat(result.output).doesNotContain("io.spring.dependency-management")
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

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.output).contains("Configuration cache entry stored.")

    val resultWithConfigurationCache = runner.build()
    assertThat(resultWithConfigurationCache.task(verifyTask)?.outcome)
      .isEqualTo(TaskOutcome.SUCCESS)
    assertThat(resultWithConfigurationCache.output).contains("Configuration cache entry reused.")
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

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.SKIPPED)
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
            sentry.verifyOpenTelemetryVersions = false
            """
        .trimIndent()
    )

    val result = runner.appendArguments("app:verifySentryOpenTelemetryVersions").build()

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.SKIPPED)
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

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output)
      .contains("OpenTelemetry was downgraded below the version its integration requires")
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

    assertThat(result.task(verifyTask)?.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output)
      .contains(
        "io.opentelemetry.semconv:opentelemetry-semconv: Sentry requires 1.42.0 but 1.41.0 was resolved"
      )
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
