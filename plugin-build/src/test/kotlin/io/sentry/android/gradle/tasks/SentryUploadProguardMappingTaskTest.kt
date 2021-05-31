package io.sentry.android.gradle.tasks

import java.io.File
import java.lang.IllegalStateException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryUploadProguardMappingTaskTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `cli-executable is set correctly`() {
        val randomUuid = UUID.randomUUID()
        createFakeUuid(randomUuid)

        val project = createProject()
        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidDirectory.set(tempDir.root)
                it.mappingsFiles = mappingFile
                it.autoUpload.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertTrue("upload-proguard" in args)
        assertTrue("--uuid" in args)
        assertTrue(randomUuid.toString() in args)
        assertTrue(mappingFile.get().first().toString() in args)
        assertFalse("--no-upload" in args)
    }

    @Test
    fun `--auto-upload is set correctly`() {
        createFakeUuid()
        val project = createProject()
        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidDirectory.set(tempDir.root)
                it.mappingsFiles = mappingFile
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
        createFakeUuid()
        val project = createProject()
        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidDirectory.set(tempDir.root)
                it.mappingsFiles = mappingFile
                it.autoUpload.set(false)
                it.sentryOrganization.set("dummy-org")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        createFakeUuid()
        val project = createProject()
        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidDirectory.set(tempDir.root)
                it.mappingsFiles = mappingFile
                it.autoUpload.set(false)
                it.sentryProject.set("dummy-proj")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--project" in args)
        assertTrue("dummy-proj" in args)
    }

    @Test
    fun `readUuidFromFile works correctly`() {
        val expected = "8c776014-bb25-11eb-8529-0242ac130003"
        val input = tempDir.newFile().apply { writeText("io.sentry.ProguardUuids=$expected") }
        val actual = SentryUploadProguardMappingsTask.readUuidFromFile(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `readUuidFromFile works correctly with whitespaces`() {
        val expected = "8c776014-bb25-11eb-8529-0242ac130003"
        val input = tempDir.newFile().apply { writeText(" io.sentry.ProguardUuids=$expected\n") }
        val actual = SentryUploadProguardMappingsTask.readUuidFromFile(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `readUuidFromFile fails with missing file`() {
        assertThrows(IllegalStateException::class.java) {
            SentryUploadProguardMappingsTask.readUuidFromFile(File("missing"))
        }
    }

    @Test
    fun `readUuidFromFile fails with empty file`() {
        assertThrows(IllegalStateException::class.java) {
            SentryUploadProguardMappingsTask.readUuidFromFile(tempDir.newFile())
        }
    }

    @Test
    fun `readUuidFromFile fails with missing property`() {
        assertThrows(IllegalStateException::class.java) {
            val inputFile = tempDir.newFile().apply { writeText("a.property=true") }
            SentryUploadProguardMappingsTask.readUuidFromFile(inputFile)
        }
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }

    private fun createFakeUuid(uuid: UUID = UUID.randomUUID()) {
        tempDir.newFile("sentry-debug-meta.properties").writeText(
            "io.sentry.ProguardUuids=$uuid"
        )
    }

    private fun createMappingFileProvider(
        project: Project,
        path: String
    ): Provider<FileCollection> = project.providers.provider { project.files(path) }
}
