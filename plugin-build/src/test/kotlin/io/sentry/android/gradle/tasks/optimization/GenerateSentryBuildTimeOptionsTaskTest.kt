package io.sentry.android.gradle.tasks.optimization

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GenerateSentryBuildTimeOptionsTaskTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `generates build time options source`() {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("project")).build()
    val outputDir = tempDir.newFolder("generated")
    val availabilityFile = tempDir.newFile("class-availability.json")
    val metadataFile = tempDir.newFile("manifest-metadata.json")
    val manifest =
      tempDir.newFile("AndroidManifest.xml").apply {
        writeText(
          """
          <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
              <meta-data android:name="io.sentry.debug" android:value="true"/>
              <meta-data android:name="io.sentry.dsn" android:value="quoted&amp;&quot;value"/>
            </application>
          </manifest>
          """
            .trimIndent()
        )
      }
    val availabilityTask =
      project.tasks.register(
        "resolveAvailability",
        ResolveSentryClassAvailabilityTask::class.java,
      ) {
        it.moduleIds.set(setOf("com.jakewharton.timber:timber", "androidx.core:core"))
        it.outputFile.set(availabilityFile)
      }
    val metadataTask =
      project.tasks.register("parseMetadata", ParseSentryManifestMetadataTask::class.java) {
        it.mergedManifest.set(manifest)
        it.outputFile.set(metadataFile)
      }
    val generateTask =
      project.tasks.register("generateOptions", GenerateSentryBuildTimeOptionsTask::class.java) {
        it.classAvailabilityFile.set(availabilityFile)
        it.manifestMetadataFile.set(metadataFile)
        it.output.set(outputDir)
      }

    availabilityTask.get().resolve()
    metadataTask.get().parse()
    generateTask.get().generate()

    val source =
      outputDir.resolve("io/sentry/android/core/SentryGeneratedBuildTimeOptions.java").readText()
    assertThat(source).contains("availability.put(\"timber.log.Timber\", true);")
    assertThat(source).contains("availability.put(\"androidx.core.view.ScrollingView\", true);")
    assertThat(source).contains("availability.put(\"androidx.lifecycle.Lifecycle\", false);")
    assertThat(source).contains("metadata.put(\"io.sentry.debug\", true);")
    assertThat(source).contains("metadata.put(\"io.sentry.dsn\", \"quoted&\\\"value\");")
  }

  @Test
  fun `preserves manifest metadata fallback`() {
    assertThat(generateSource("null")).contains("    return null;")
  }

  @Test
  fun `falls back when manifest metadata type cannot be inferred`() {
    val telemetry = mock<SentryTelemetryService>()

    assertThat(generateSource("""{"io.sentry.max-breadcrumbs":"0xFFFFFFFFFFFFFFFF"}""", telemetry))
      .contains("    return null;")
    verify(telemetry).captureHandledError(any(), eq("manifest metadata type inference"))
  }

  @Test
  fun `preserves authoritative empty manifest metadata`() {
    val source = generateSource("{}")

    assertThat(source).contains("Map<String, Object> metadata = new HashMap<>();")
    assertThat(source).doesNotContain("metadata.put(")
  }

  @Test
  fun `register does not resolve runtime classpath`() {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("java-project")).build()
    project.plugins.apply("java")
    val runtimeClasspath = project.configurations.getByName("runtimeClasspath")

    GenerateSentryBuildTimeOptionsTask.register(
        project,
        "runtimeClasspath",
        "Test",
        project.layout.file(project.provider { project.file("AndroidManifest.xml") }),
      )
      ?.get()

    assertThat(runtimeClasspath.state).isEqualTo(Configuration.State.UNRESOLVED)
  }

  @Test
  fun `register includes project dependencies`() {
    val root = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("root")).build()
    val replayDir = root.file("sentry-android-replay").apply { mkdir() }
    val replay =
      ProjectBuilder.builder()
        .withName("sentry-android-replay")
        .withProjectDir(replayDir)
        .withParent(root)
        .build()
    replay.group = "io.sentry"
    replay.plugins.apply("java")
    val appDir = root.file("app").apply { mkdir() }
    val app =
      ProjectBuilder.builder().withName("app").withProjectDir(appDir).withParent(root).build()
    app.plugins.apply("java")
    app.dependencies.add(
      "implementation",
      app.dependencies.project(mapOf("path" to ":sentry-android-replay")),
    )

    GenerateSentryBuildTimeOptionsTask.register(
        app,
        "runtimeClasspath",
        "Test",
        app.layout.file(app.provider { app.file("AndroidManifest.xml") }),
      )!!
      .get()
    val task =
      app.tasks
        .named("resolveSentryClassAvailabilityTest", ResolveSentryClassAvailabilityTask::class.java)
        .get()

    assertThat(task.moduleIds.get()).contains("io.sentry:sentry-android-replay")
  }

  private fun generateSource(metadata: String, telemetry: SentryTelemetryService? = null): String {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder()).build()
    val availabilityFile = tempDir.newFile().apply { writeText("{}") }
    val metadataFile = tempDir.newFile().apply { writeText(metadata) }
    val outputDir = tempDir.newFolder()
    val task =
      project.tasks.register("generateOptions", GenerateSentryBuildTimeOptionsTask::class.java) {
        it.classAvailabilityFile.set(availabilityFile)
        it.manifestMetadataFile.set(metadataFile)
        it.output.set(outputDir)
        telemetry?.let { service -> it.sentryTelemetryService.set(service) }
      }

    task.get().generate()

    return outputDir
      .resolve("io/sentry/android/core/SentryGeneratedBuildTimeOptions.java")
      .readText()
  }
}
