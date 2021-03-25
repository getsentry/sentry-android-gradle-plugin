package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryTasksProvider.getAssembleTask
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getDexTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageTask
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getTransformerTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SentryTaskProviderTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    @Test
    fun `getTransformerTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getTransformerTask(project, "debug"))
    }

    @Test
    fun `getTransformerTask returns transform for R8`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("transformClassesAndResourcesWithR8ForDebug")

        assertEquals(task.get(), getTransformerTask(project, "debug"))
    }

    @Test
    fun `getTransformerTask returns transform for Proguard`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("transformClassesAndResourcesWithProguardForDebug")

        assertEquals(task.get(), getTransformerTask(project, "debug"))
    }

    @Test
    fun `getTransformerTask returns minify for R8`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("minifyDebugWithR8")

        assertEquals(task.get(), getTransformerTask(project, "debug"))
    }

    @Test
    fun `getTransformerTask returns minify for Proguard`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("minifyDebugWithProguard")

        assertEquals(task.get(), getTransformerTask(project, "debug"))
    }

    @Test
    fun `getDexTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getDexTask(project, "debug"))
    }

    @Test
    fun `getDexTask returns transform with Dex`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("transformClassesWithDexForDebug")

        assertEquals(task.get(), getDexTask(project, "debug"))
    }

    @Test
    fun `getDexTask returns transform with Dex builder`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("transformClassesWithDexBuilderForDebug")

        assertEquals(task.get(), getDexTask(project, "debug"))
    }

    @Test
    fun `getDexTask returns transform with Dex shrinker`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("transformClassesAndDexWithShrinkResForDebug")

        assertEquals(task.get(), getDexTask(project, "debug"))
    }

    @Test
    fun `getPreBundleTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getPreBundleTask(project, "debug"))
    }

    @Test
    fun `getPreBundleTask returns correct task`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("buildDebugPreBundle")

        assertEquals(task.get(), getPreBundleTask(project, "debug"))
    }

    @Test
    fun `getBundleTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getBundleTask(project, "debug"))
    }

    @Test
    fun `getBundleTask returns correct task`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("bundleDebug")

        assertEquals(task.get(), getBundleTask(project, "debug"))
    }

    @Test
    fun `getPackageTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getPackageTask(project, "debug"))
    }

    @Test
    fun `getPackageTask returns plain package task`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("packageDebug")

        assertEquals(task.get(), getPackageTask(project, "debug"))
    }

    @Test
    fun `getPackageTask returns package bundle task`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("packageDebugBundle")

        assertEquals(task.get(), getPackageTask(project, "debug"))
    }

    @Test
    fun `getAssembleTask works correctly for all the variants`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }

        // This forces the project to be evaluated
        project.getTasksByName("assembleDebug", false)

        android.applicationVariants.all {
            if (it.name == "debug") {
                assertEquals("assembleDebug", getAssembleTask(project, it).name)
            } else {
                assertEquals("assembleRelease", getAssembleTask(project, it).name)
            }
        }
    }
}
