package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.sourcecontext.SourceCollector
import java.io.File
import kotlin.test.assertEquals
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

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

        val sourceDirs = project.files()
        sourceDirs.from("dummy/src/a")
        sourceDirs.from("dummy/src/b")

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
