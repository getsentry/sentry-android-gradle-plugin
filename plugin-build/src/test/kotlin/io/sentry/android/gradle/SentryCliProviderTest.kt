package io.sentry.android.gradle

import io.sentry.android.gradle.SentryCliProvider.getCliSuffix
import io.sentry.android.gradle.SentryCliProvider.getSentryPropertiesPath
import io.sentry.android.gradle.SentryCliProvider.loadCliFromResources
import io.sentry.android.gradle.SentryCliProvider.searchCliInPropertiesFile
import io.sentry.android.gradle.SentryCliProvider.searchCliInResources
import io.sentry.android.gradle.util.SystemPropertyRule
import io.sentry.android.gradle.util.WithSystemProperty
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SentryCliProviderTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    @get:Rule
    val systemPropertyRule = SystemPropertyRule()

    @Test
    fun `getSentryPropertiesPath returns local properties file`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        testProjectDir.newFile("sentry.properties")

        assertEquals(
            project.file("sentry.properties").path,
            getSentryPropertiesPath(project.projectDir, project.rootDir)
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
            getSentryPropertiesPath(project.projectDir, project.rootDir)
        )
    }

    @Test
    fun `getSentryPropertiesPath returns null if no properties file is found`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        assertNull(getSentryPropertiesPath(project.projectDir, project.rootDir))
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

        assertEquals("vim", searchCliInPropertiesFile(project.projectDir, project.rootDir))
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

        assertNull(searchCliInPropertiesFile(project.projectDir, project.rootDir))
    }

    @Test
    fun `searchCliInPropertiesFile returns null for empty file`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        testProjectDir.newFile("sentry.properties")

        assertNull(searchCliInPropertiesFile(project.projectDir, project.rootDir))
    }

    @Test
    fun `searchCliInPropertiesFile returns null for missing file`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(testProjectDir.root)
            .build()

        assertNull(searchCliInPropertiesFile(project.projectDir, project.rootDir))
    }

    @Test
    fun `searchCliInResources finds the file correctly`() {
        val resourcePath = "./dummy-bin/dummy-sentry-cli"
        val resourceFile = javaClass.getResource(".")
            ?.let { File(it.file, resourcePath) }
            ?.apply {
                parentFile.mkdirs()
                createNewFile()
            }

        val foundPath = searchCliInResources(resourcePath)
        assertNotNull(foundPath)
        assertTrue(
            foundPath.endsWith("${File.separator}dummy-bin${File.separator}dummy-sentry-cli")
        )

        resourceFile?.delete()
    }

    @Test
    fun `searchCliInResources returns null if file does not exist`() {
        val resourcePath = "./dummy-bin/i-dont-exist"

        assertNull(searchCliInResources(resourcePath))
    }

    @Test
    fun `loadCliFromResourcesToTemp finds the file correctly`() {
        val resourcePath = "./dummy-bin/dummy-sentry-cli"
        val resourceFile = javaClass.getResource(".")
            ?.let { File(it.file, resourcePath) }
            ?.apply {
                parentFile.mkdirs()
                createNewFile()
                writeText("echo \"This is just a dummy script\"")
            }

        val loadedPath = loadCliFromResources(File("."), resourcePath)
        assertNotNull(loadedPath)

        val binContent = File(loadedPath).readText()
        assertEquals("echo \"This is just a dummy script\"", binContent)

        resourceFile?.delete()
    }

    @Test
    fun `loadCliFromResourcesToTemp returns null if file does not exist`() {
        val resourcePath = "./dummy-bin/i-dont-exist"

        assertNull(loadCliFromResources(File("."), resourcePath))
    }

    @Test
    @WithSystemProperty(["os.name"], ["mac"])
    fun `getCliSuffix on mac returns Darwin-universal`() {
        assertEquals("Darwin-universal", getCliSuffix())
    }

    @Test
    @WithSystemProperty(["os.name", "os.arch"], ["linux", "amd64"])
    fun `getCliSuffix on linux amd64 returns Linux-x86_64`() {
        assertEquals("Linux-x86_64", getCliSuffix())
    }

    @Test
    @WithSystemProperty(["os.name", "os.arch"], ["linux", "armV7"])
    fun `getCliSuffix on linux armV7 returns Linux-armV7`() {
        assertEquals("Linux-armV7", getCliSuffix())
    }

    @Test
    @WithSystemProperty(["os.name"], ["windows"])
    fun `getCliSuffix on win returns Windows-i686`() {
        assertEquals("Windows-i686.exe", getCliSuffix())
    }

    @Test
    @WithSystemProperty(["os.name"], ["¯\\_(ツ)_/¯"])
    fun `getCliSuffix on an unknown platform returns null`() {
        assertNull(getCliSuffix())
    }
}
