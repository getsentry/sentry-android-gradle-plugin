package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryTasksProvider.getAssembleTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getInstallTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getLintVitalAnalyzeProvider
import io.sentry.android.gradle.SentryTasksProvider.getLintVitalReportProvider
import io.sentry.android.gradle.SentryTasksProvider.getMergeAssetsProvider
import io.sentry.android.gradle.SentryTasksProvider.getMinifyTasks
import io.sentry.android.gradle.SentryTasksProvider.getPackageBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageProvider
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getProcessResourcesProvider
import io.sentry.gradle.common.SentryVariant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryTaskProviderTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    @Test
    fun `getTransformerTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertTrue(getMinifyTasks(project, "debug").isEmpty())
    }

    @Test
    fun `getTransformerTask returns transform task for standalone Proguard with opt-in`() {
        val (project, task) = getTestProjectWithTask(
            "transformClassesAndResourcesWithProguardTransformForDebug"
        )

        assertEquals(
            task,
            getMinifyTasks(
                project,
                "debug",
                dexguardEnabled = true
            ).first().get()
        )
    }

    @Test
    fun `getTransformerTask returns null for standalone Proguard without opt-in`() {
        val (project, _) = getTestProjectWithTask(
            "transformClassesAndResourcesWithProguardTransformForDebug"
        )

        assertTrue(
            getMinifyTasks(
                project,
                "debug",
                dexguardEnabled = false
            ).isEmpty()
        )
    }

    @Test
    fun `getTransformerTask returns minify for R8`() {
        val (project, task) = getTestProjectWithTask("minifyDebugWithR8")

        assertEquals(task, getMinifyTasks(project, "debug").first().get())
    }

    @Test
    fun `getTransformerTask returns minify for embedded Proguard`() {
        val (project, task) = getTestProjectWithTask("minifyDebugWithProguard")

        assertEquals(task, getMinifyTasks(project, "debug").first().get())
    }

    @Test
    fun `getTransformerTask gives standalone Proguard priority with opt-in`() {
        val (project, _) = getTestProjectWithTask("minifyDebugWithR8")
        project.tasks.register("transformClassesAndResourcesWithProguardTransformForDebug")

        assertEquals(
            "transformClassesAndResourcesWithProguardTransformForDebug",
            getMinifyTasks(
                project,
                "debug",
                dexguardEnabled = true
            ).first().name
        )
    }

    @Test
    fun `getTransformerTask ignores standalone Proguard priority without opt-in`() {
        val (project, r8task) = getTestProjectWithTask("minifyDebugWithR8")
        project.tasks.register("transformClassesAndResourcesWithProguardTransformForDebug")

        assertEquals(
            r8task,
            getMinifyTasks(
                project,
                "debug",
                dexguardEnabled = false
            ).first().get()
        )
    }

    @Test
    fun `getPreBundleTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getPreBundleTask(project, "debug")?.get())
    }

    @Test
    fun `getPreBundleTask returns correct task`() {
        val (project, task) = getTestProjectWithTask("buildDebugPreBundle")

        assertEquals(task, getPreBundleTask(project, "debug")?.get())
    }

    @Test
    fun `getBundleTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getBundleTask(project, "debug")?.get())
    }

    @Test
    fun `getBundleTask returns correct task`() {
        val (project, task) = getTestProjectWithTask("bundleDebug")

        assertEquals(task, getBundleTask(project, "debug")?.get())
    }

    @Test
    fun `getPackageBundleTask returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getPackageBundleTask(project, "debug")?.get())
    }

    @Test
    fun `getPackageBundleTask returns package bundle task`() {
        val (project, task) = getTestProjectWithTask("packageDebugBundle")

        assertEquals(task, getPackageBundleTask(project, "debug")?.get())
    }

    @Test
    fun `getAssembleTaskProvider works correctly for all the variants AGP70`() {
        val (project, android) = getAndroidExtFromProject()

        android.applicationVariants.configureEach {
            if (it.name == "debug") {
                assertEquals(
                    "assembleDebug",
                    getAssembleTaskProvider(project, AndroidVariant70(it))?.get()?.name
                )
            } else {
                assertEquals(
                    "assembleRelease",
                    getAssembleTaskProvider(project, AndroidVariant70(it))?.get()?.name
                )
            }
        }
    }

    @Test
    fun `getAssembleTaskProvider falls back to findTask if assembleProvider is null`() {
        val (project, android) = getAndroidExtFromProject()

        android.applicationVariants.configureEach {
            val sentryVariant = object : SentryVariant by AndroidVariant70(it) {
                override val assembleProvider: TaskProvider<out Task>? get() = null
            }
            if (it.name == "debug") {
                assertEquals(
                    "assembleDebug",
                    getAssembleTaskProvider(project, sentryVariant)?.get()?.name
                )
            } else {
                assertEquals(
                    "assembleRelease",
                    getAssembleTaskProvider(project, sentryVariant)?.get()?.name
                )
            }
        }
    }

    @Test
    fun `getInstallTaskProvider works correctly for all the variants AGP70`() {
        val (project, android) = getAndroidExtFromProject()

        android.applicationVariants.configureEach {
            if (it.name == "debug") {
                assertEquals(
                    "installDebug",
                    getInstallTaskProvider(project, AndroidVariant70(it))?.get()?.name
                )
            }
        }
    }

    @Test
    fun `getInstallTaskProvider falls back to findTask if assembleProvider is null`() {
        val (project, android) = getAndroidExtFromProject()

        android.applicationVariants.configureEach {
            val sentryVariant = object : SentryVariant by AndroidVariant70(it) {
                override val installProvider: TaskProvider<out Task>? get() = null
            }
            if (it.name == "debug") {
                assertEquals(
                    "installDebug",
                    getInstallTaskProvider(project, sentryVariant)?.get()?.name
                )
            }
        }
    }

    @Test
    fun `getMergeAssetsProvider works correctly for all the variants`() {
        val (_, android) = getAndroidExtFromProject()

        android.applicationVariants.configureEach {
            if (it.name == "debug") {
                assertEquals("mergeDebugAssets", getMergeAssetsProvider(it)?.get()?.name)
            } else {
                assertEquals("mergeReleaseAssets", getMergeAssetsProvider(it)?.get()?.name)
            }
        }
    }

    @Test
    fun `getPackageProvider works correctly for all the variants`() {
        val (_, android) = getAndroidExtFromProject()

        android.applicationVariants.configureEach {
            if (it.name == "debug") {
                assertEquals("packageDebug", getPackageProvider(AndroidVariant70(it))?.name)
            } else {
                assertEquals("packageRelease", getPackageProvider(AndroidVariant70(it))?.name)
            }
        }
    }

    @Test
    fun `getLintVitalAnalyze returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getLintVitalAnalyzeProvider(project, "debug")?.get())
    }

    @Test
    fun `getLintVitalAnalyze returns correct task`() {
        val (project, task) = getTestProjectWithTask("lintVitalAnalyzeDebug")

        assertEquals(task, getLintVitalAnalyzeProvider(project, "debug")?.get())
    }

    @Test
    fun `getLintVitalReport returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getLintVitalReportProvider(project, "debug")?.get())
    }

    @Test
    fun `getLintVitalReport returns correct task`() {
        val (project, task) = getTestProjectWithTask("lintVitalReportDebug")

        assertEquals(task, getLintVitalReportProvider(project, "debug")?.get())
    }

    @Test
    fun `getProcessResources returns null for missing task`() {
        val project = ProjectBuilder.builder().build()

        assertNull(getProcessResourcesProvider(project)?.get())
    }

    @Test
    fun `getProcessResources returns correct task`() {
        val (project, task) = getTestProjectWithTask("processResources")

        assertEquals(task, getProcessResourcesProvider(project)?.get())
    }

    private fun getAndroidExtFromProject(): Pair<Project, AppExtension> {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }

        // This forces the project to be evaluated
        project.getTasksByName("assembleDebug", false)
        return project to android
    }

    private fun getTestProjectWithTask(taskName: String): Pair<Project, Task> {
        val project = ProjectBuilder.builder().build()
        return project to project.tasks.register(taskName).get()
    }
}
