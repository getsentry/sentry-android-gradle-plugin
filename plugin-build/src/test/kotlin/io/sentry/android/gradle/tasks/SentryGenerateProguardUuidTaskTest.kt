package io.sentry.android.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SentryGenerateProguardUuidTaskTest {

    @Test
    fun `generate proguard UUID sets the output file correctly`() {

        val project = createProject()
        val task: TaskProvider<SentryGenerateProguardUuidTask> =
            project.tasks.register("testGenerateProguardUuid", SentryGenerateProguardUuidTask::class.java) {
                it.outputDirectory.set(project.file("dummy/folder/"))
            }

        assertEquals(
            project.file("dummy/folder/sentry-debug-meta.properties"),
            task.get().outputFile.get().asFile
        )
    }

    @Test
    fun `generate proguard UUID generates the UUID correctly`() {

        val project = createProject()
        val task: TaskProvider<SentryGenerateProguardUuidTask> =
            project.tasks.register("testGenerateProguardUuid", SentryGenerateProguardUuidTask::class.java) {
                it.outputDirectory.set(project.file("dummy/folder/"))
            }

        task.get().generateProperties()

        val expectedFile = File(project.projectDir, "dummy/folder/sentry-debug-meta.properties")
        assertTrue(expectedFile.exists())
        assertTrue(expectedFile.readText().startsWith("io.sentry.ProguardUuids="))
    }

    @Test
    fun `generate proguard UUID overrides the UUID on subsequent calls`() {

        val project = createProject()
        val task: TaskProvider<SentryGenerateProguardUuidTask> =
            project.tasks.register("testGenerateProguardUuid", SentryGenerateProguardUuidTask::class.java) {
                it.outputDirectory.set(project.file("dummy/folder/"))
            }
        val expectedFile = File(project.projectDir, "dummy/folder/sentry-debug-meta.properties")

        task.get().generateProperties()

        val uuid1 = expectedFile.readText()

        task.get().generateProperties()

        val uuid2 = expectedFile.readText()

        assertTrue(uuid1.startsWith("io.sentry.ProguardUuids="))
        assertTrue(uuid2.startsWith("io.sentry.ProguardUuids="))
        assertNotEquals(uuid1, uuid2)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
