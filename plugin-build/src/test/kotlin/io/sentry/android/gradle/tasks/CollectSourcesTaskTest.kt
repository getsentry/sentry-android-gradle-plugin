package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.sourcecontext.BundleSourcesTask
import io.sentry.android.gradle.sourcecontext.CollectSourcesTask
import io.sentry.android.gradle.sourcecontext.SourceCollector
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

class CollectSourcesTaskTest {

    @Test
    fun `cli-executable is set correctly`() {
        val project = createProject()

        project.file("dummy/src/a/TestFile1.java").also {
            it.parentFile.mkdirs()
            it.writeText("TestFile1")
        }
        project.file("dummy/src/a/child/TestFile2.java").also {
            it.parentFile.mkdirs()
            it.writeText("TestFile2")
        }
        project.file("dummy/src/b/TestFile3.java").also {
            it.parentFile.mkdirs()
            it.writeText("TestFile3")
        }

        val sourceDirs = listOf(
            project.files("dummy/src/a"),
            project.files("dummy/src/b")
        )

        val outDir = File(project.buildDir, "dummy/out")

        SourceCollector().collectSources(outDir, sourceDirs)

        val outSources = outDir.walk().filter { it.isFile }.toList()
        assertEquals(3, outSources.size)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
