package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class GenerateBundleIdTaskTest {

    @Test
    fun `generate bundleId generates ID correctly`() {
        val project = createProject()
        val task: TaskProvider<GenerateBundleIdTask> =
            GenerateBundleIdTask.register(
                project,
                project.extensions.findByName("sentry") as SentryPluginExtension,
                null,
                project.layout.buildDirectory.dir("dummy/folder/"),
                project.objects.property(Boolean::class.java).convention(true),
                "test"
            )

        task.get().generateProperties()

        val expectedFile = File(project.buildDir, "dummy/folder/sentry-bundle-id.properties")
        assertTrue(expectedFile.exists())

        val props = PropertiesUtil.load(expectedFile)
        val bundleId = props.getProperty(SENTRY_BUNDLE_ID_PROPERTY)
        assertNotNull(bundleId)
    }

    @Test
    fun `generate bundleId overrides the ID on subsequent calls`() {
        val project = createProject()
        val task: TaskProvider<GenerateBundleIdTask> =
            GenerateBundleIdTask.register(
                project,
                project.extensions.findByName("sentry") as SentryPluginExtension,
                null,
                project.layout.buildDirectory.dir("dummy/folder/"),
                project.objects.property(Boolean::class.java).convention(true),
                "test"
            )
        val expectedFile = File(project.buildDir, "dummy/folder/sentry-bundle-id.properties")

        task.get().generateProperties()

        val props1 = PropertiesUtil.load(expectedFile)
        val bundleId1 = props1.getProperty(SENTRY_BUNDLE_ID_PROPERTY)

        task.get().generateProperties()

        val props2 = PropertiesUtil.load(expectedFile)
        val bundleId2 = props2.getProperty(SENTRY_BUNDLE_ID_PROPERTY)

        assertNotNull(bundleId1)
        assertNotNull(bundleId2)
        assertNotEquals(bundleId1, bundleId2)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
