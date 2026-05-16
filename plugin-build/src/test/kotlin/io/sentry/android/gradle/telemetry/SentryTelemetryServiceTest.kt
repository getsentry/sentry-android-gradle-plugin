package io.sentry.android.gradle.telemetry

import io.sentry.android.gradle.extensions.SentryPluginExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryTelemetryServiceTest {

  @get:Rule val testProjectDir = TemporaryFolder()

  @Test
  fun `createParameters with null url returns saas true`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)

    val params =
      SentryTelemetryService.createParameters(
        project,
        null,
        extension,
        project.provider { "/fake/sentry-cli" },
        null,
        "Android",
      )

    assertEquals(true, params.saas)
  }

  @Test
  fun `createParameters with sentry_io url returns saas true`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)
    extension.url.set("https://sentry.io")

    val params =
      SentryTelemetryService.createParameters(
        project,
        null,
        extension,
        project.provider { "/fake/sentry-cli" },
        null,
        "Android",
      )

    assertEquals(true, params.saas)
  }

  @Test
  fun `createParameters with custom url returns saas false`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)
    extension.url.set("https://sentry.mycompany.com")

    val params =
      SentryTelemetryService.createParameters(
        project,
        null,
        extension,
        project.provider { "/fake/sentry-cli" },
        null,
        "Android",
      )

    assertEquals(false, params.saas)
  }

  @Test
  fun `createParameters with telemetry disabled does not resolve cliExecutable`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)
    extension.telemetry.set(false)

    var providerResolved = false
    val cliExecutable: Provider<String> = project.provider {
      providerResolved = true
      "/fake/sentry-cli"
    }

    val params =
      SentryTelemetryService.createParameters(
        project,
        null,
        extension,
        cliExecutable,
        null,
        "Android",
      )

    assertNull(params.cliExecutable)
    assertEquals(false, providerResolved)
  }

  @Test
  fun `createParameters with telemetry enabled populates CLI config`() {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val extension = project.extensions.create("sentry", SentryPluginExtension::class.java)
    extension.authToken.set("test-token")

    val params =
      SentryTelemetryService.createParameters(
        project,
        null,
        extension,
        project.provider { "/fake/sentry-cli" },
        null,
        "Android",
      )

    assertEquals("/fake/sentry-cli", params.cliExecutable)
    assertNotNull(params.buildDirectory)
    assertEquals("test-token", params.authToken)
    assertTrue(params.sendTelemetry)
  }
}
