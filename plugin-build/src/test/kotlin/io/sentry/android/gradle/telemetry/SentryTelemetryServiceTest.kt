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

  @Test
  fun `reads Kotlin Gradle plugin version`() {
    assertThat(SentryTelemetryService.pluginVersion(FakeKotlinGradlePlugin())).isEqualTo("2.3.0")
  }

  @Test
  fun `ignores plugins without a version`() {
    assertThat(SentryTelemetryService.pluginVersion(Any())).isNull()
  }

  @Test
  fun `createParameters omits Kotlin Gradle plugin version when Kotlin is not applied`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)

    val params = SentryTelemetryService.createParameters(project, null, extension, null, "test")

    assertThat(params.extraTags).doesNotContainKey("KGP_VERSION")
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

class FakeKotlinGradlePlugin {
  @Suppress("unused") fun getPluginVersion(): String = "2.3.0"
}
