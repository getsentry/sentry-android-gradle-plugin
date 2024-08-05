package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask.Companion.SENTRY_PROGUARD_MAPPING_UUID_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import java.util.UUID
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
                SentryGenerateProguardUuidTask::class.java
            ) {
                it.output.set(project.layout.buildDirectory.dir("dummy/folder/"))
            }

        task.get().generateProperties()

        val expectedFile = File(project.buildDir, "dummy/folder/sentry-proguard-uuid.properties")
        assertTrue(expectedFile.exists())

        val props = PropertiesUtil.load(expectedFile)
        val uuid = props.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY)
        assertNotNull(uuid)
        UUID.fromString(uuid)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
