package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.ReleaseInfo
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
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
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project, randomUuid)
        val releaseInfo = ReleaseInfo("com.test", "1.0.0", 1)

        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFile
                it.autoUploadProguardMapping.set(true)
                it.releaseInfo.set(releaseInfo)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertTrue("upload-proguard" in args)
        assertTrue("--uuid" in args)
        assertTrue(randomUuid.toString() in args)
        assertTrue(mappingFile.get().first().toString() in args)
        assertTrue("--app-id" in args)
        assertTrue(releaseInfo.applicationId in args)
        assertTrue("--version" in args)
        assertTrue(releaseInfo.versionName in args)
        assertTrue("--version-code" in args)
        assertTrue(releaseInfo.versionCode.toString() in args)
        assertFalse("--no-upload" in args)
    }

    @Test
    fun `with no version code cli-executable is set correctly`() {
        val randomUuid = UUID.randomUUID()
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project, randomUuid)
        val releaseInfo = ReleaseInfo("com.test", "1.0.0")

        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFile
                it.autoUploadProguardMapping.set(true)
                it.releaseInfo.set(releaseInfo)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertTrue("upload-proguard" in args)
        assertTrue("--uuid" in args)
        assertTrue(randomUuid.toString() in args)
        assertTrue(mappingFile.get().first().toString() in args)
        assertTrue("--app-id" in args)
        assertTrue(releaseInfo.applicationId in args)
        assertTrue("--version" in args)
        assertTrue(releaseInfo.versionName in args)
        assertFalse("--version-code" in args)
        assertFalse("--no-upload" in args)
        assertFalse("--log-level=debug" in args)
    }

    @Test
    fun `with multiple mappingFiles picks the first existing file`() {
        val randomUuid = UUID.randomUUID()
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project, randomUuid)
        val releaseInfo = ReleaseInfo("com.test", "1.0.0")

        val mappingFiles = createMappingFileProvider(
            project,
            "dummy/folder/missing-mapping.txt",
            "dummy/folder/existing-mapping.txt"
        )
        val existingFile = project.file("dummy/folder/existing-mapping.txt").apply {
            parentFile.mkdirs()
            writeText("dummy-file")
        }

        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFiles
                it.autoUploadProguardMapping.set(true)
                it.releaseInfo.set(releaseInfo)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue(existingFile.toString() in args)
    }

    @Test
    fun `--auto-upload is set correctly`() {
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project)
        val releaseInfo = ReleaseInfo("com.test", "1.0.0")

        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFile
                it.autoUploadProguardMapping.set(false)
                it.releaseInfo.set(releaseInfo)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--no-upload" in args)
    }

    @Test
    fun `--log-level=debug is set correctly`() {
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project)

        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFile
                it.autoUploadProguardMapping.set(false)
                it.debug.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--log-level=debug" in args)
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
    fun `with sentryAuthToken env variable is set correctly`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.sentryAuthToken.set("<token>")
            }

        task.get().setSentryAuthTokenEnv()

        assertEquals(
            "<token>",
            task.get().environment["SENTRY_AUTH_TOKEN"].toString()
        )
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project)

        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFile
                it.autoUploadProguardMapping.set(false)
                it.sentryOrganization.set("dummy-org")
                it.releaseInfo.set(ReleaseInfo("com.test", "1.0.0", 1))
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val uuidFileProvider = createFakeUuid(project)

        val mappingFile = createMappingFileProvider(project, "dummy/folder/mapping.txt")
        val task: TaskProvider<SentryUploadProguardMappingsTask> =
            project.tasks.register(
                "testUploadProguardMapping",
                SentryUploadProguardMappingsTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.uuidFile.set(uuidFileProvider)
                it.mappingsFiles = mappingFile
                it.autoUploadProguardMapping.set(false)
                it.sentryProject.set("dummy-proj")
                it.releaseInfo.set(ReleaseInfo("com.test", "1.0.0", 1))
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

    private fun createFakeUuid(
        project: Project,
        uuid: UUID = UUID.randomUUID()
    ): Provider<RegularFile> {
        val file = tempDir.newFile("sentry-debug-meta.properties").apply {
            writeText("io.sentry.ProguardUuids=$uuid")
        }
        return project.layout.file(project.provider { file })
    }

    private fun createMappingFileProvider(
        project: Project,
        vararg path: String
    ): Provider<FileCollection> = project.providers.provider { project.files(*path) }
}
