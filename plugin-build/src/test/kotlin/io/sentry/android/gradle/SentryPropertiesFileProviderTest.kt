package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class SentryPropertiesFileProviderTest {

    private val sep = File.separator

    @Test
    fun `getPropertiesFilePath finds file inside debug folder`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }
        project.evaluate()
        createTestFile(project.projectDir, "src/debug/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside project folder`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }
        project.evaluate()
        createTestFile(project.projectDir, "sentry.properties")

        val variant = android.applicationVariants.first()

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside flavorName folder`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version")
            productFlavors.create("lite")
            productFlavors.create("full")
        }
        project.evaluate()
        createTestFile(project.projectDir, "src/lite/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside flavorName-buildType folder`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version")
            productFlavors.create("lite")
            productFlavors.create("full")
        }
        project.evaluate()
        createTestFile(project.projectDir, "src/lite/debug/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside buildType-flavorName folder`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version")
            productFlavors.create("lite")
            productFlavors.create("full")
        }
        project.evaluate()
        createTestFile(project.projectDir, "src/debug/lite/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath with multiple flavorDimensions finds file inside flavor folder`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version", "api")
            productFlavors.create("lite") {
                it.dimension("version")
            }
            productFlavors.create("api30") {
                it.dimension("api")
            }
        }
        project.evaluate()
        createTestFile(project.projectDir, "src/liteApi30/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteApi30Debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside other productFlavor folders`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version", "api")
            productFlavors.create("lite") {
                it.dimension("version")
            }
            productFlavors.create("api30") {
                it.dimension("api")
            }
        }
        project.evaluate()
        createTestFile(project.projectDir, "src/api30/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteApi30Debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root project folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }
        project.evaluate()
        createTestFile(rootProject.projectDir, "sentry.properties")

        val variant = android.applicationVariants.first()

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root buildType folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }
        project.evaluate()
        createTestFile(rootProject.projectDir, "src/debug/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root flavor folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version")
            productFlavors.create("lite")
        }
        project.evaluate()
        createTestFile(rootProject.projectDir, "src/lite/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root flavor buildType folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version")
            productFlavors.create("lite")
        }
        project.evaluate()
        createTestFile(rootProject.projectDir, "src/lite/debug/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root buildType flavor folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            flavorDimensions("version")
            productFlavors.create("lite")
        }
        project.evaluate()
        createTestFile(rootProject.projectDir, "src/debug/lite/sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    private fun Project.evaluate() {
        project.getTasksByName("assembleDebug", false)
    }

    private fun createTestFile(parent: File, path: String) =
        File(parent, path).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("42")
        }
}
