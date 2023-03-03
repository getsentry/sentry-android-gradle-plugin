package io.sentry.android.gradle.tasks

import java.io.File
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryWriteIntegrationListToManifestTaskTest {

    @Test
    fun `modify manifest`() {
        val project = createProject()
        val outputDir = File(project.buildDir, "dummy/folder/").apply {
            mkdirs()
        }
        val inputManifest = File(outputDir, "AndroidManifestInput.xml").apply {
            createNewFile()
            writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application android:name=".SampleApp">
                    </application>
                </manifest>
                """.trimIndent()
            )
        }
        val outputManifest = File(outputDir, "AndroidManifest.xml").apply {
            createNewFile()
        }
        val task: TaskProvider<SentryGenerateIntegrationListTask> =
            project.tasks.register(
                "testWriteIntegrationListToManifest",
                SentryGenerateIntegrationListTask::class.java
            ) {
                it.integrations.set(listOf("one", "two"))
                it.mergedManifest.set(inputManifest)
                it.updatedManifest.set(outputManifest)
            }

        task.get().writeIntegrationListToManifest()
        assertTrue(
            outputManifest.readText().contains(
                """<meta-data android:name="io.sentry.integrations" android:value="one,two"/>"""
            )
        )
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
