package io.sentry.android.gradle.util

import io.sentry.android.gradle.testutil.createTestAndroidProject
import io.sentry.android.gradle.testutil.createTestProguardProject
import io.sentry.android.gradle.util.SentryPluginUtils.capitalizeUS
import io.sentry.android.gradle.util.SentryPluginUtils.getAndDeleteFile
import io.sentry.android.gradle.util.SentryPluginUtils.isMinificationEnabled
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import proguard.gradle.plugin.android.dsl.ProGuardAndroidExtension

class SentryPluginUtilsTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `capitalizes string first letter uppercase`() {
        assertEquals("Kotlin", "kotlin".capitalizeUS())
    }

    @Test
    fun `capitalizes string does nothing on already capitalized string`() {
        assertEquals("Kotlin", "Kotlin".capitalizeUS())
    }

    @Test
    fun `capitalizes string returns empty on empty string`() {
        assertEquals("", "".capitalizeUS())
    }

    @Test
    fun `isMinificationEnabled returns false for standalone Proguard`() {
        val (project, android) = createTestProguardProject()
        val debug = android.applicationVariants.first { it.name == "debug" }

        assertEquals(
            false,
            isMinificationEnabled(project, debug, experimentalGuardsquareSupport = true)
        )
    }

    @Test
    fun `isMinificationEnabled returns true for standalone Proguard and valid config`() {
        val (project, android) = createTestProguardProject()
        project.extensions.getByType(ProGuardAndroidExtension::class.java).apply {
            configurations.create("debug") {
                it.defaultConfiguration("proguard-android-optimize.txt")
            }
        }
        val debug = android.applicationVariants.first { it.name == "debug" }

        assertEquals(
            true,
            isMinificationEnabled(project, debug, experimentalGuardsquareSupport = true)
        )
    }

    @Test
    fun `isMinificationEnabled returns false for standalone Proguard without opt-in`() {
        val (project, android) = createTestProguardProject()
        project.extensions.getByType(ProGuardAndroidExtension::class.java).apply {
            configurations.create("debug") {
                it.defaultConfiguration("proguard-android-optimize.txt")
            }
        }
        val debug = android.applicationVariants.first { it.name == "debug" }

        assertEquals(
            false,
            isMinificationEnabled(project, debug, experimentalGuardsquareSupport = false)
        )
    }

    @Test
    fun `isMinificationEnabled returns false for debug builds`() {
        val (project, android) = createTestAndroidProject()
        val debug = android.applicationVariants.first { it.name == "debug" }

        assertEquals(false, isMinificationEnabled(project, debug))
    }

    @Test
    fun `getAndDelete deletes the file`() {
        val (project, _) = createTestAndroidProject()
        val file = tempDir.newFile("temp-file.txt")

        assertTrue { file.exists() }

        getAndDeleteFile(project.layout.file(project.provider { file }))
        assertFalse { file.exists() }
    }
}
