package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.SentryCliProvider
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryCliExecTaskTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()

        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertFalse("--org" in args)
        assertFalse("--project" in args)
        assertFalse("--log-level=debug" in args)
    }

    @Test
    fun `cli-executable is extracted from resources if  required`() {
        val project = createProject()

        val cliPath = SentryCliProvider.getCliFromResourcesExtractionPath(project.buildDir)

        assertTrue(!cliPath.exists())

        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set(cliPath.absolutePath)
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
            }

        // when the args are computed (usually during task execution)
        val args = task.get().computeCommandLineArgs()

        // then the CLI should be extracted and set
        assertTrue(cliPath.exists())
        assertEquals(cliPath.absolutePath, File(args[0]).absolutePath)
    }

    @Test
    fun `--log-level=debug is set correctly`() {
        val project = createProject()

        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.debug.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--log-level=debug" in args)
    }

    @Test
    fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
        val project = createProject()
        val propertiesFile = project.file("dummy/folder/sentry.properties")
        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sentryProperties.set(propertiesFile)
            }

        task.get().setSentryPropertiesEnv()

        assertEquals(
            propertiesFile.absolutePath,
            task.get().environment["SENTRY_PROPERTIES"].toString()
        )
    }

    @Test
    fun `with sentryAuthToken env variable is set correctly`() {
        val project = createProject()
        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sentryAuthToken.set("<token>")
            }

        task.get().setSentryAuthTokenEnv()

        assertEquals(
            "<token>",
            task.get().environment["SENTRY_AUTH_TOKEN"].toString()
        )
    }

    @Test
    fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
        val project = createProject()
        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
            }

        task.get().setSentryPropertiesEnv()

        assertNull(task.get().environment["SENTRY_PROPERTIES"])
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sentryOrganization.set("dummy-org")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sentryProject.set("dummy-proj")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--project" in args)
        assertTrue("dummy-proj" in args)
    }

    @Test
    fun `with sentryUrl --url is set`() {
        val project = createProject()
        val task: TaskProvider<TestTask> =
            project.tasks.register(
                "testTask",
                TestTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sentryUrl.set("https://some-host.sentry.io")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--url" in args)
        assertTrue("https://some-host.sentry.io" in args)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }

    abstract class TestTask : SentryCliExecTask() {
        override fun getArguments(args: MutableList<String>) {
            // no-op
        }
    }
}
