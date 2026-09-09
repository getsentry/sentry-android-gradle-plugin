package io.sentry.android.gradle.tasks

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.tasks.SentryOpenTelemetryVersionChecker.VersionDowngrade
import org.junit.Test

class SentryOpenTelemetryVersionCheckerTest {
  @Test
  fun `identifies Sentry OpenTelemetry artifacts`() {
    assertThat(
        SentryOpenTelemetryVersionChecker.isSentryOpenTelemetryArtifact(
          "io.sentry",
          "sentry-opentelemetry-agentless",
        )
      )
      .isTrue()

    assertThat(
        SentryOpenTelemetryVersionChecker.isSentryOpenTelemetryArtifact("io.sentry", "sentry")
      )
      .isFalse()
    assertThat(
        SentryOpenTelemetryVersionChecker.isSentryOpenTelemetryArtifact(
          "io.opentelemetry",
          "sentry-opentelemetry-agentless",
        )
      )
      .isFalse()
  }

  @Test
  fun `builds Spring dependency management guidance with concrete BOM version`() {
    val message =
      SentryOpenTelemetryVersionChecker.buildMessage(
        downgrades = listOf(downgrade()),
        docsUrl = "https://docs.example.com",
        springDependencyManagementApplied = true,
      )

    assertThat(message).contains("io.opentelemetry:opentelemetry-sdk")
    assertThat(message).contains("Sentry requires 1.63.0 but 1.62.0 was resolved")
    assertThat(message).contains("Requested by: io.sentry:sentry-opentelemetry-agentless:2.0.0")
    assertThat(message).contains("Gradle selection reason: forced")
    assertThat(message).contains("mavenBom(\"io.sentry:sentry-opentelemetry-bom:2.0.0\")")
    assertThat(message).doesNotContain("<sentryVersion>")
    assertThat(message).doesNotContain("implementation(platform")
  }

  @Test
  fun `builds Gradle platform guidance with concrete BOM version`() {
    val message =
      SentryOpenTelemetryVersionChecker.buildMessage(
        downgrades = listOf(downgrade()),
        docsUrl = "https://docs.example.com",
        springDependencyManagementApplied = false,
      )

    assertThat(message)
      .contains("implementation(platform(\"io.sentry:sentry-opentelemetry-bom:2.0.0\"))")
    assertThat(message).doesNotContain("<sentryVersion>")
    assertThat(message).doesNotContain("dependencyManagement")
  }

  private fun downgrade(): VersionDowngrade =
    VersionDowngrade(
      module = "io.opentelemetry:opentelemetry-sdk",
      requested = "1.63.0",
      resolved = "1.62.0",
      requestedBy = "io.sentry:sentry-opentelemetry-agentless:2.0.0",
      sentryBomVersion = "2.0.0",
      reason = "forced",
    )
}
