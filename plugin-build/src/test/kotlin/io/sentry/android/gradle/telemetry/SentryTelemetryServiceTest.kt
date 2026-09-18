package io.sentry.android.gradle.telemetry

import com.google.common.truth.Truth.assertThat
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

  // KGP is compileOnly and therefore absent from the test classpath, which is exactly the
  // situation of a build with no Kotlin plugin: the version read must degrade to null instead
  // of letting a NoClassDefFoundError escape.
  @Test
  fun `kotlin plugin version is null when KGP is not on the classpath`() {
    assertThat(SentryTelemetryService.kotlinPluginVersion()).isNull()
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
