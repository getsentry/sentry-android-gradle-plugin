package io.sentry.android.gradle.tasks

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryUploadNativeSymbolsTaskTest {

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()
        val task = createTestTask(project) {
            it.buildDir.set(project.buildDir)
            it.cliExecutable.set("sentry-cli")
            it.includeNativeSources.set(false)
            it.variantName.set("debug")
            it.autoUploadNativeSymbol.set(true)
        }

        val args = task.computeCommandLineArgs()
        val sep = File.separator

        assertTrue("sentry-cli" in args)
        assertTrue("upload-dif" in args)
        val path = "${project.buildDir}${sep}intermediates" +
            "${sep}merged_native_libs${sep}debug"
        assertTrue(path in args)
        assertFalse("--include-sources" in args)
    }

    @Test
    fun `--auto-upload is set correctly`() {
        val project = createProject()
        val task = createTestTask(project) {
            it.buildDir.set(project.buildDir)
            it.cliExecutable.set("sentry-cli")
            it.includeNativeSources.set(false)
            it.variantName.set("debug")
            it.autoUploadNativeSymbol.set(false)
        }

        val args = task.computeCommandLineArgs()

        assertTrue("--no-upload" in args)
    }

    @Test
    fun `--include-sources is set correctly`() {
        val project = createProject()
        val task = createTestTask(project) {
            it.buildDir.set(project.buildDir)
            it.cliExecutable.set("sentry-cli")
            it.includeNativeSources.set(true)
            it.variantName.set("debug")
            it.autoUploadNativeSymbol.set(true)
        }

        val args = task.computeCommandLineArgs()

        assertTrue("--include-sources" in args)
    }

    @Test
    fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
        val project = createProject()
        val propertiesFile = project.file("dummy/folder/sentry.properties")
        val task = createTestTask(project) {
            it.sentryProperties.set(propertiesFile)
        }

        task.setSentryPropertiesEnv()

        assertEquals(propertiesFile.absolutePath, task.environment["SENTRY_PROPERTIES"].toString())
    }

    @Test
    fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
        val project = createProject()
        val task = createTestTask(project)

        task.setSentryPropertiesEnv()

        assertNull(task.environment["SENTRY_PROPERTIES"])
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val task = createTestTask(project) {
            it.buildDir.set(project.buildDir)
            it.cliExecutable.set("sentry-cli")
            it.sentryOrganization.set("dummy-org")
            it.includeNativeSources.set(true)
            it.variantName.set("debug")
            it.autoUploadNativeSymbol.set(true)
        }

        val args = task.computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val task = createTestTask(project) {
            it.buildDir.set(project.buildDir)
            it.cliExecutable.set("sentry-cli")
            it.sentryProject.set("dummy-proj")
            it.includeNativeSources.set(true)
            it.variantName.set("debug")
            it.autoUploadNativeSymbol.set(true)
        }

        val args = task.computeCommandLineArgs()

        assertTrue("--project" in args)
        assertTrue("dummy-proj" in args)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }

    private fun createTestTask(
        project: Project,
        block: (SentryUploadNativeSymbolsTask) -> Unit = {}
    ): SentryUploadNativeSymbolsTask =
        project.tasks.register(
            "testUploadNativeSymbols",
            SentryUploadNativeSymbolsTask::class.java
        ) { block(it) }.get()
}
