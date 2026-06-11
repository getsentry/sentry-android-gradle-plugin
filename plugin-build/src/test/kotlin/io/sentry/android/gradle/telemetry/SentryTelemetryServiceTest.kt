package io.sentry.android.gradle.telemetry

import io.sentry.BuildConfig
import io.sentry.android.gradle.extensions.SentryPluginExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryTelemetryServiceTest {

  @get:Rule val testProjectDir = TemporaryFolder()

  @Test
  fun `createParameters uses BuildConfig CliVersion`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)

    val params = SentryTelemetryService.createParameters(project, null, extension, null, "test")

    assertEquals(BuildConfig.CliVersion, params.cliVersion)
  }

  @Test
  fun `createParameters detects SaaS when no URL is set`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)

    val params = SentryTelemetryService.createParameters(project, null, extension, null, "test")

    assertTrue(params.saas == true)
  }

  @Test
  fun `createParameters detects self-hosted when URL is set`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)
    extension.url.set("https://sentry.example.com")

    val params = SentryTelemetryService.createParameters(project, null, extension, null, "test")

    assertTrue(params.saas == false)
  }
}
