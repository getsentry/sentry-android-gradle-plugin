package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.sourcecontext.BundleSourcesTask
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import java.io.File
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BundleSourcesTaskTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("sentry-cli" in args)
        assertTrue("debug-files" in args)
        assertTrue("bundle-jvm" in args)
        assertTrue(sourceDir.absolutePath in args)
        assertTrue("--output=${outDir.absolutePath}" in args)

        assertFalse("--org" in args)
        assertFalse("--project" in args)
        assertFalse("--log-level=debug" in args)
    }

    @Test
    fun `--log-level=debug is set correctly`() {
        val project = createProject()
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
                it.debug.set(true)
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--log-level=debug" in args)
    }

    @Test
    fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
        val project = createProject()
        val propertiesFile = project.file("dummy/folder/sentry.properties")
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
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
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
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
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
            }

        task.get().setSentryPropertiesEnv()

        assertNull(task.get().environment["SENTRY_PROPERTIES"])
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
                it.sentryOrganization.set("dummy-org")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
                it.sentryProject.set("dummy-proj")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--project" in args)
        assertTrue("dummy-proj" in args)
    }

    @Test
    fun `readBundleIdFromFile works correctly`() {
        val expected = "8c776014-bb25-11eb-8529-0242ac130003"
        val input = tempDir.newFile().apply { writeText("$SENTRY_BUNDLE_ID_PROPERTY=$expected") }
        val actual = BundleSourcesTask.readBundleIdFromFile(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `readBundleIdFromFile works correctly with whitespaces`() {
        val expected = "8c776014-bb25-11eb-8529-0242ac130003"
        val input = tempDir.newFile().apply { writeText(" $SENTRY_BUNDLE_ID_PROPERTY=$expected\n") }
        val actual = BundleSourcesTask.readBundleIdFromFile(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `readBundleIdFromFile fails with missing file`() {
        assertThrows(IllegalStateException::class.java) {
            BundleSourcesTask.readBundleIdFromFile(File("missing"))
        }
    }

    @Test
    fun `readBundleIdFromFile fails with empty file`() {
        assertThrows(IllegalStateException::class.java) {
            BundleSourcesTask.readBundleIdFromFile(tempDir.newFile())
        }
    }

    @Test
    fun `readBundleIdFromFile fails with missing property`() {
        assertThrows(IllegalStateException::class.java) {
            val inputFile = tempDir.newFile().apply { writeText("a.property=true") }
            BundleSourcesTask.readBundleIdFromFile(inputFile)
        }
    }

    @Test
    fun `with sentryUrl --url is set`() {
        val project = createProject()
        val debugMetaPropertiesFile = createDebugMetaProperties(project)

        val sourceDir = File(project.buildDir, "dummy/source")
        val outDir = File(project.buildDir, "dummy/out")
        val task: TaskProvider<BundleSourcesTask> =
            project.tasks.register(
                "testBundleSources",
                BundleSourcesTask::class.java
            ) {
                it.cliExecutable.set("sentry-cli")
                it.buildDirectory.set(project.layout.buildDirectory.asFile)
                it.sourceDir.set(sourceDir)
                it.bundleIdFile.set(debugMetaPropertiesFile)
                it.output.set(outDir)
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

    private fun createDebugMetaProperties(
        project: Project,
        uuid: UUID = UUID.randomUUID()
    ): Provider<RegularFile> {
        val file = tempDir.newFile("sentry-debug-meta.properties").apply {
            Properties().also { props ->
                props.setProperty(SENTRY_BUNDLE_ID_PROPERTY, uuid.toString())
                this.writer().use { props.store(it, "") }
            }
        }
        return project.layout.file(project.provider { file })
    }
}
