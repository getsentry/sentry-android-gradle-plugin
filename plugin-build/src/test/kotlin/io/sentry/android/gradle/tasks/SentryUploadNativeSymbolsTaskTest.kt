package io.sentry.android.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SentryUploadNativeSymbolsTaskTest {

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadNativeSymbolsTask> =
            project.tasks.register("testUploadNativeSymbols", SentryUploadNativeSymbolsTask::class.java) {
                it.cliExecutable.set("sentry-cli")
                it.includeNativeSources.set(false)
                it.variantName.set("debug")
            }

        val args = task.get().computeCommandLineArgs()
        val sep = File.separator

        assertTrue("sentry-cli" in args)
        assertTrue("upload-dif" in args)
        assertTrue("${project.projectDir}${sep}build${sep}intermediates${sep}merged_native_libs${sep}debug" in args)
        assertFalse("--include-sources" in args)
    }

    @Test
    fun `--include-sources is set correctly`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadNativeSymbolsTask> =
            project.tasks.register("testUploadNativeSymbols", SentryUploadNativeSymbolsTask::class.java) {
                it.cliExecutable.set("sentry-cli")
                it.includeNativeSources.set(true)
                it.variantName.set("debug")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--include-sources" in args)
    }

    @Test
    fun `with sentryProperties file SENTRY_PROPERTIES is set correctly`() {
        val project = createProject()
        val propertiesFile = project.file("dummy/folder/sentry.properties")
        val task: TaskProvider<SentryUploadNativeSymbolsTask> =
            project.tasks.register("testUploadNativeSymbols", SentryUploadNativeSymbolsTask::class.java) {
                it.sentryProperties.set(propertiesFile)
            }

        task.get().setSentryPropertiesEnv()

        assertEquals(propertiesFile.absolutePath, task.get().environment["SENTRY_PROPERTIES"].toString())
    }

    @Test
    fun `without sentryProperties file SENTRY_PROPERTIES is not set`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadNativeSymbolsTask> =
            project.tasks.register("testUploadNativeSymbols", SentryUploadNativeSymbolsTask::class.java)

        task.get().setSentryPropertiesEnv()

        assertNull(task.get().environment["SENTRY_PROPERTIES"])
    }

    @Test
    fun `with sentryOrganization adds --org`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadNativeSymbolsTask> =
            project.tasks.register("testUploadNativeSymbols", SentryUploadNativeSymbolsTask::class.java) {
                it.cliExecutable.set("sentry-cli")
                it.sentryOrganization.set("dummy-org")
                it.includeNativeSources.set(true)
                it.variantName.set("debug")
            }

        val args = task.get().computeCommandLineArgs()

        assertTrue("--org" in args)
        assertTrue("dummy-org" in args)
    }

    @Test
    fun `with sentryProject adds --project`() {
        val project = createProject()
        val task: TaskProvider<SentryUploadNativeSymbolsTask> =
            project.tasks.register("testUploadNativeSymbols", SentryUploadNativeSymbolsTask::class.java) {
                it.cliExecutable.set("sentry-cli")
                it.sentryProject.set("dummy-proj")
                it.includeNativeSources.set(true)
                it.variantName.set("debug")
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
