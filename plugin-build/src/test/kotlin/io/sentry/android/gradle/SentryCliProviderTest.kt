package io.sentry.android.gradle

import io.sentry.android.gradle.SentryCliProvider.getCliSuffix
import io.sentry.android.gradle.SentryCliProvider.getSentryPropertiesPath
import io.sentry.android.gradle.SentryCliProvider.loadCliFromResourcesToTemp
import io.sentry.android.gradle.SentryCliProvider.searchCliInPropertiesFile
import io.sentry.android.gradle.SentryCliProvider.searchCliInResources
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SentryCliProviderTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    @Test
    fun `getSentryPropertiesPath returns local properties file`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        testProjectDir.newFile("sentry.properties")

        assertEquals(
            project.file("sentry.properties").path,
            getSentryPropertiesPath(project)
        )
    }

    @Test
    fun `getSentryPropertiesPath fallbacks to top level properties file`() {
        val topLevelProject = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        val project = ProjectBuilder
            .builder()
            .withParent(topLevelProject)
            .build()

        testProjectDir.newFile("sentry.properties")

        assertEquals(
            topLevelProject.file("sentry.properties").path,
            getSentryPropertiesPath(project)
        )
    }

    @Test
    fun `getSentryPropertiesPath returns null if no properties file is found`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        assertNull(getSentryPropertiesPath(project))
    }

    @Test
    fun `searchCliInPropertiesFile returns cli-executable correctly`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        testProjectDir.newFile("sentry.properties").apply {
            writeText("cli.executable=vim")
        }

        assertEquals("vim", searchCliInPropertiesFile(project))
    }

    @Test
    fun `searchCliInPropertiesFile ignores other fields`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        testProjectDir.newFile("sentry.properties").apply {
            writeText("another=field")
        }

        assertNull(searchCliInPropertiesFile(project))
    }

    @Test
    fun `searchCliInPropertiesFile returns null for empty file`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        testProjectDir.newFile("sentry.properties")

        assertNull(searchCliInPropertiesFile(project))
    }

    @Test
    fun `searchCliInPropertiesFile returns null for missing file`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        assertNull(searchCliInPropertiesFile(project))
    }

    @Test
    fun `searchCliInResources finds the file correctly`() {
        val resourcePath = "/testFixtures/bin/dummy-sentry-cli"

        val foundPath = searchCliInResources(resourcePath)
        assertNotNull(foundPath)
        assertTrue(foundPath.endsWith(resourcePath))
    }

    @Test
    fun `searchCliInResources returns null if file does not exist`() {
        val resourcePath = "/testFixtures/bin/i-dont-exist"

        assertNull(searchCliInResources(resourcePath))
    }

    @Test
    fun `loadCliFromResourcesToTemp finds the file correctly`() {
        val resourcePath = "/testFixtures/bin/dummy-sentry-cli"

        val loadedPath = loadCliFromResourcesToTemp(resourcePath)
        assertNotNull(loadedPath)

        val binContent = File(loadedPath).readText()
        assertEquals(
            """
            #!/bin/bash
            echo "This is just a dummy script"

            """.trimIndent(),
            binContent
        )
    }

    @Test
    fun `loadCliFromResourcesToTemp returns null if file does not exist`() {
        val resourcePath = "/testFixtures/bin/i-dont-exist"

        assertNull(loadCliFromResourcesToTemp(resourcePath))
    }

    @Test
    fun `getCliSuffix on mac returns Darwin`() {
        System.setProperty("os.name", "mac")

        assertEquals("Darwin-x86_64", getCliSuffix())
    }

    @Test
    fun `getCliSuffix on linux amd64 returns Linux-x86_64`() {
        System.setProperty("os.name", "linux")
        System.setProperty("os.arch", "amd64")

        assertEquals("Linux-x86_64", getCliSuffix())
    }

    @Test
    fun `getCliSuffix on linux armV7 returns Linux-armV7`() {
        System.setProperty("os.name", "linux")
        System.setProperty("os.arch", "armV7")

        assertEquals("Linux-armV7", getCliSuffix())
    }

    @Test
    fun `getCliSuffix on win returns Windows-i686`() {
        System.setProperty("os.name", "windows")

        assertEquals("Windows-i686.exe", getCliSuffix())
    }

    @Test
    fun `getCliSuffix on an unknown platform returns null`() {
        System.setProperty("os.name", "¯\\_(ツ)_/¯")

        assertNull(getCliSuffix())
    }
}
