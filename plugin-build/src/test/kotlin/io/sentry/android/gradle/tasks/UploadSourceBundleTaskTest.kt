package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.sourcecontext.UploadSourceBundleTask
import java.io.File
import java.util.Properties
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

class UploadSourceBundleTaskTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()

        val sourceBundleDir = File(project.buildDir, "dummy/folder")
        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertTrue("debug-files" in args)
        assertTrue("upload" in args)
        assertTrue("--type=jvm" in args)
        assertTrue(sourceBundleDir.absolutePath in args)

        assertFalse("--no-upload" in args)
        assertFalse("--org" in args)
        assertFalse("--project" in args)
        assertFalse("--log-level=debug" in args)
    }

    @Test
    fun `--auto-upload is set correctly`() {
        val project = createProject()

        val sourceBundleDir = File(project.buildDir, "dummy/folder")
        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(false)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--no-upload" in args)
    }

    @Test
    fun `--log-level=debug is set correctly`() {
        val project = createProject()

        val sourceBundleDir = File(project.buildDir, "dummy/folder")
        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(true)
                it.debug.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--log-level=debug" in args)
    }

    @Test
    fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
        val project = createProject()
        val propertiesFile = project.file("dummy/folder/sentry.properties")
        val sourceBundleDir = File(project.buildDir, "dummy/folder")

        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(true)
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
        val sourceBundleDir = File(project.buildDir, "dummy/folder")

        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(true)
            }

        task.get().setSentryPropertiesEnv()

        assertNull(task.get().environment["SENTRY_PROPERTIES"])
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val sourceBundleDir = File(project.buildDir, "dummy/folder")

        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(true)
                it.sentryOrganization.set("dummy-org")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val sourceBundleDir = File(project.buildDir, "dummy/folder")

        val task: TaskProvider<UploadSourceBundleTask> =
            project.tasks.register(
                "testUploadSourceBundle",
                UploadSourceBundleTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.sourceBundleDir.set(sourceBundleDir)
                it.autoUploadSourceBundle.set(true)
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
