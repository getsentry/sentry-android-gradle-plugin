package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask.Companion.SENTRY_PROGUARD_MAPPING_UUID_PROPERTY
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryGenerateDebugMetaPropertiesTaskTest {

    @Test
    fun `generate debug-meta properties generates proguard mapping UUID and bundle id correctly`() {
        val project = createProject()
        val sourceDirs = project.files()
        sourceDirs.from("dummy/src/a")
        val bundleIdTask = GenerateBundleIdTask.register(
            project,
            project.extensions.findByName("sentry") as SentryPluginExtension,
            null,
            null,
            project.layout.buildDirectory.dir("dummy/folder/"),
            project.objects.property(Boolean::class.java).convention(true),
            "test"
        )
        val proguardIdTask = SentryGenerateProguardUuidTask.register(
            project,
            project.extensions.findByName("sentry") as SentryPluginExtension,
            null,
            project.layout.buildDirectory.dir("dummy/folder/"),
            null,
            "test"
        )
        val idGenerationTasks = listOf(
            bundleIdTask,
            proguardIdTask
        )
        val task: TaskProvider<SentryGenerateDebugMetaPropertiesTask> =
            SentryGenerateDebugMetaPropertiesTask.register(
                project,
                project.extensions.findByName("sentry") as SentryPluginExtension,
                null,
                idGenerationTasks,
                project.layout.buildDirectory.dir("dummy/folder/"),
                "test"
            )

        bundleIdTask.get().generateProperties()
        proguardIdTask.get().generateProperties()
        task.get().generateProperties()

        val expectedFile = File(project.buildDir, "dummy/folder/sentry-debug-meta.properties")
        assertTrue(expectedFile.exists())

        val props = PropertiesUtil.load(expectedFile)
        assertNotNull(props.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY))
        assertNotNull(props.getProperty(SENTRY_BUNDLE_ID_PROPERTY))
    }

    @Test
    fun `generate proguard UUID overrides the UUID on subsequent calls`() {
        val project = createProject()
        val bundleIdTask = GenerateBundleIdTask.register(
            project,
            project.extensions.findByName("sentry") as SentryPluginExtension,
            null,
            null,
            project.layout.buildDirectory.dir("dummy/folder/"),
            project.objects.property(Boolean::class.java).convention(true),
            "test"
        )
        val proguardIdTask = SentryGenerateProguardUuidTask.register(
            project,
            project.extensions.findByName("sentry") as SentryPluginExtension,
            null,
            project.layout.buildDirectory.dir("dummy/folder/"),
            null,
            "test"
        )
        val idGenerationTasks = listOf(
            bundleIdTask,
            proguardIdTask
        )
        val task: TaskProvider<SentryGenerateDebugMetaPropertiesTask> =
            SentryGenerateDebugMetaPropertiesTask.register(
                project,
                project.extensions.findByName("sentry") as SentryPluginExtension,
                null,
                idGenerationTasks,
                project.layout.buildDirectory.dir("dummy/folder/"),
                "test"
            )

        bundleIdTask.get().generateProperties()
        proguardIdTask.get().generateProperties()
        task.get().generateProperties()

        val expectedFile = File(project.buildDir, "dummy/folder/sentry-debug-meta.properties")
        assertTrue(expectedFile.exists())

        val props1 = PropertiesUtil.load(expectedFile)
        val proguardId1 = props1.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY)
        assertNotNull(proguardId1)
        val bundleId1 = props1.getProperty(SENTRY_BUNDLE_ID_PROPERTY)
        assertNotNull(bundleId1)

        bundleIdTask.get().generateProperties()
        proguardIdTask.get().generateProperties()
        task.get().generateProperties()

        assertTrue(expectedFile.exists())

        val props2 = PropertiesUtil.load(expectedFile)
        val proguardId2 = props2.getProperty(SENTRY_PROGUARD_MAPPING_UUID_PROPERTY)
        assertNotNull(proguardId2)
        val bundleId2 = props2.getProperty(SENTRY_BUNDLE_ID_PROPERTY)
        assertNotNull(bundleId2)
    }

    private fun createProject(): Project {
        with(ProjectBuilder.builder().build()) {
            plugins.apply("io.sentry.android.gradle")
            return this
        }
    }
}
