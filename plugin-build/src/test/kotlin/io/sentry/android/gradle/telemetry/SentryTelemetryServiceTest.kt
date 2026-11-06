package io.sentry.android.gradle.telemetry

import com.google.common.truth.Truth.assertThat
import io.sentry.BuildConfig
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.util.getBuildServiceName
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryTelemetryServiceTest {

  @get:Rule val testProjectDir = TemporaryFolder()

  private fun createService(): SentryTelemetryService {
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    return project.gradle.sharedServices
      .registerIfAbsent(
        getBuildServiceName(SentryTelemetryService::class.java),
        SentryTelemetryService::class.java,
      ) {}
      .get()
  }

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

  @Test
  fun `unwrapGradleException returns plain exception as-is`() {
    val service = createService()
    val original = RuntimeException("plain error")

    assertThat(service.unwrapGradleException(original)).isSameInstanceAs(original)
  }

  @Test
  fun `unwrapGradleException unwraps TaskExecutionException`() {
    val service = createService()
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val task = project.task("dummyTask")
    val realCause = RuntimeException("real task error")
    val wrapped = TaskExecutionException(task, realCause)

    assertThat(service.unwrapGradleException(wrapped)).isSameInstanceAs(realCause)
  }

  @Test
  fun `unwrapGradleException unwraps UncheckedException`() {
    val service = createService()
    val realCause = Exception("checked error")
    // UncheckedException has no public constructor; use the static factory which throws
    val unchecked =
      try {
        org.gradle.internal.UncheckedException.throwAsUncheckedException(realCause)
        null
      } catch (e: RuntimeException) {
        e
      }!!

    assertThat(service.unwrapGradleException(unchecked)).isSameInstanceAs(realCause)
  }

  @Test
  fun `unwrapGradleException unwraps nested wrapper chain`() {
    val service = createService()
    val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
    val task = project.task("dummyTask2")
    val realCause = RuntimeException("deep cause")
    val taskException = TaskExecutionException(task, realCause)
    // In composite builds Gradle creates UncheckedException via its package-private constructor
    val constructor =
      Class.forName("org.gradle.internal.UncheckedException")
        .getDeclaredConstructor(Throwable::class.java)
    constructor.isAccessible = true
    val unchecked = constructor.newInstance(taskException) as RuntimeException

    assertThat(service.unwrapGradleException(unchecked)).isSameInstanceAs(realCause)
  }

  @Test
  fun `unwrapGradleException does not unwrap non-wrapper exception with cause`() {
    val service = createService()
    val cause = RuntimeException("inner")
    val outer = IllegalStateException("outer", cause)

    assertThat(service.unwrapGradleException(outer)).isSameInstanceAs(outer)
  }
}
