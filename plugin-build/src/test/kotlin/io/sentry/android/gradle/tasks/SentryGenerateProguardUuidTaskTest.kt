package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask.Companion.SENTRY_PROGUARD_MAPPING_UUID_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryGenerateProguardUuidTaskTest {

  @Test
  fun `generate proguard UUID generates the UUID correctly`() {
    val project = createProject()
    val task: TaskProvider<SentryGenerateProguardUuidTask> =
      project.tasks.register(
        "testGenerateProguardUuid",
        SentryGenerateProguardUuidTask::class.java,
      ) {
        it.output.set(project.layout.buildDirectory.dir("dummy/folder/"))
        it.fallbackMappingFiles.from(
          File(project.buildDir, "mapping.txt").also { file ->
            file.parentFile.mkdirs()
            file.writeText("mapping")
          }
        )
      }

    task.get().generateProperties()

    val expectedFile = File(project.buildDir, "dummy/folder/sentry-proguard-uuid.properties")
    assertTrue(expectedFile.exists())

    val props = PropertiesUtil.load(expectedFile)
    val uuid = props.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY)
    assertNotNull(uuid)
    UUID.fromString(uuid)
  }

  @Test
  fun `generate proguard UUID removes stale output when no mapping exists`() {
    val project = createProject()
    val task =
      project.tasks.register(
        "testRemoveStaleProguardUuid",
        SentryGenerateProguardUuidTask::class.java,
      ) {
        it.output.set(project.layout.buildDirectory.dir("dummy/folder/"))
      }
    val staleOutput =
      File(project.buildDir, "dummy/folder/sentry-proguard-uuid.properties").also {
        it.parentFile.mkdirs()
        it.writeText("$SENTRY_PROGUARD_MAPPING_UUID_PROPERTY=stale")
      }

    task.get().generateProperties()

    assertFalse(staleOutput.exists())
  }

  private fun createProject(): Project {
    with(ProjectBuilder.builder().build()) {
      plugins.apply("io.sentry.android.gradle")
      return this
    }
  }
}
