package io.sentry.android.gradle.tasks

import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryUploadProguardMappingTaskTest {

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()
        val randomUuid = UUID.randomUUID()
        val mappingFile = project.file("dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.mappingsUuid.set(randomUuid)
                it.mappingsFile.set(mappingFile)
                it.autoUpload.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertTrue("upload-proguard" in args)
        assertTrue("--uuid" in args)
        assertTrue(randomUuid.toString() in args)
        assertTrue(mappingFile.toString() in args)
        assertFalse("--no-upload" in args)
    }

    @Test
    fun `--auto-upload is set correctly`() {
        val project = createProject()
        val randomUuid = UUID.randomUUID()
        val mappingFile = project.file("dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.mappingsUuid.set(randomUuid)
                it.mappingsFile.set(mappingFile)
                it.autoUpload.set(false)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--no-upload" in args)
    }

    @Test
    fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
        val project = createProject()
        val propertiesFile = project.file("dummy/folder/sentry.properties")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.sentryProperties.set(propertiesFile)
            }

        task.get().setSentryPropertiesEnv()

        assertEquals(
            propertiesFile.absolutePath,
            task.get().environment["SENTRY_PROPERTIES"].toString()
        )
    }

    @Test
    fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            )

        task.get().setSentryPropertiesEnv()

        assertNull(task.get().environment["SENTRY_PROPERTIES"])
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.mappingsUuid.set(UUID.randomUUID())
                it.mappingsFile.set(project.file("dummy/folder/mapping.txt"))
                it.autoUpload.set(false)
                it.sentryOrganization.set("dummy-org")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.mappingsUuid.set(UUID.randomUUID())
                it.mappingsFile.set(project.file("dummy/folder/mapping.txt"))
                it.autoUpload.set(false)
                it.sentryProject.set("dummy-proj")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--project" in args)
        assertTrue("dummy-proj" in args)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
