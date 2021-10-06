package io.sentry.android.gradle

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginTest(
    private val androidGradlePluginVersion: String,
    private val gradleVersion: String
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val projectTemplateFolder = File("src/test/resources/testFixtures/appTestProject")

    private lateinit var rootBuildFile: File
    private lateinit var appBuildFile: File
    private lateinit var runner: GradleRunner

    @Before
    fun setup() {
        projectTemplateFolder.copyRecursively(testProjectDir.root)

        val pluginClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
            .joinToString(separator = ", ") { "\"$it\"" }
            .replace(File.separator, "/")

        appBuildFile = File(testProjectDir.root, "app/build.gradle")
        rootBuildFile = testProjectDir.writeFile("build.gradle") {
            // language=Groovy
            """
            buildscript {
              repositories {
                google()
                gradlePluginPortal()
              }
              dependencies {
                classpath 'com.android.tools.build:gradle:$androidGradlePluginVersion'
                // This is needed to populate the plugin classpath instead of using
                // withPluginClasspath on the Gradle Runner.
                classpath files($pluginClasspath)
              }
            }

            allprojects {
              repositories {
                google()
                mavenCentral()
              }
            }
            subprojects {
              pluginManager.withPlugin('com.android.application') {
                android {
                  compileSdkVersion 30
                  defaultConfig {
                    applicationId "com.example"
                    minSdkVersion 21
                  }
                  buildTypes {
                    release {
                      minifyEnabled true
                      proguardFiles("src/release/proguard-rules.pro")
                    }
                  }
                }
              }
            }
            """.trimIndent()
        }

        runner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--stacktrace")
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
    }

    @Test
    fun `plugin can be applied`() {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                sentry {
                  autoUpload = false
                }
            """.trimIndent()
        )

        runner.build()
    }

    @Test
    fun `plugin does not configure tasks`() {
        val prefix = "task-configured-for-test: "
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                project.tasks.configureEach { Task task -> println("$prefix" + task.path) }
            """.trimIndent()
        )

        val result = runner.withArguments("help").build()
        val configuredTasks = result.output.lines()
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .sorted()
        assertEquals(listOf(), configuredTasks)
    }

    @Test
    fun `regenerates UUID every build`() {
        runner.appendArguments(":app:assembleRelease")

        runner.build()
        val uuid1 = verifyProguardUuid(testProjectDir.root)

        runner.build()
        val uuid2 = verifyProguardUuid(testProjectDir.root)

        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun `includes a UUID in the APK`() {
        runner
            .appendArguments(":app:assembleRelease")
            .build()

        verifyProguardUuid(testProjectDir.root)
    }

    @Test
    fun `does not include a UUID in the APK`() {
        // isMinifyEnabled is disabled by default in debug builds
        runner
            .appendArguments(":app:assembleDebug")
            .build()

        assertThrows(AssertionError::class.java) {
            verifyProguardUuid(testProjectDir.root, variant = "debug", signed = false)
        }
    }

    @Test
    fun `creates uploadSentryNativeSymbols task if uploadNativeSymbols is enabled`() {
        applyUploadNativeSymbols()

        val build = runner
            .appendArguments(":app:assembleRelease", "--dry-run")
            .build()

        assertTrue(":app:uploadSentryNativeSymbolsForRelease" in build.output)
    }

    @Test
    fun `does not create uploadSentryNativeSymbols task if non debuggable app`() {
        applyUploadNativeSymbols()

        val build = runner
            .appendArguments(":app:assembleDebug", "--dry-run")
            .build()

        assertFalse(":app:uploadSentryNativeSymbolsForDebug" in build.output)
    }

    @Test
    fun `skips variant if set with ignoredVariants`() {
        applyIgnores(ignoredVariant = "release")

        val build = runner
            .appendArguments(":app:assembleRelease", "--dry-run")
            .build()

        assertFalse(":app:uploadSentryProguardMappingsRelease" in build.output)
    }

    @Test
    fun `does not skip variant if ignoredVariants specifies another value`() {
        applyIgnores(ignoredVariant = "debug")

        val build = runner
            .appendArguments(":app:assembleRelease", "--dry-run")
            .build()

        assertTrue(":app:uploadSentryProguardMappingsRelease" in build.output)
    }

    @Test
    fun `skips tracing instrumentation if tracingInstrumentation is disabled`() {
        applyTracingInstrumentation(false)

        val build = runner
            .appendArguments(":app:assembleRelease", "--dry-run")
            .build()

        assertFalse(":app:transformReleaseClassesWithAsm" in build.output)
    }

    @Test
    fun `register tracing instrumentation if tracingInstrumentation is enabled`() {
        applyTracingInstrumentation()

        val build = runner
            .appendArguments(":app:assembleRelease", "--dry-run")
            .build()

        assertTrue(":app:transformReleaseClassesWithAsm" in build.output)
    }

    private fun applyUploadNativeSymbols() {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                sentry {
                  autoUpload = false
                  uploadNativeSymbols = true
                }
            """.trimIndent()
        )
    }

    private fun applyIgnores(ignoredVariant: String) {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }
                sentry {
                  autoUpload = true
                  ignoredVariants = ["$ignoredVariant"]
                }
            """.trimIndent()
        )
    }

    private fun applyTracingInstrumentation(tracingInstrumentation: Boolean = true) {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                sentry {
                  autoUpload = false
                  tracingInstrumentation = $tracingInstrumentation
                }
            """.trimIndent()
        )
    }

    companion object {

        @Parameterized.Parameters(name = "AGP {0}, Gradle {1}")
        @JvmStatic
        fun parameters() = listOf(
            // The supported Gradle version can be found here:
            // https://developer.android.com/studio/releases/gradle-plugin#updating-gradle
            // The pair is [AGP Version, Gradle Version]
//            arrayOf("4.0.2", "6.1.1"),
//            arrayOf("4.1.3", "6.5"),
//            arrayOf("4.1.3", "6.8.3"),
//            arrayOf("4.1.3", "7.0.2"),
//            arrayOf("4.2.2", "6.8.3"),
//            arrayOf("4.2.2", "7.0.2"),
            arrayOf("7.0.2", "7.0.2"),
            arrayOf("7.0.2", "7.1.1"),
            arrayOf("7.0.2", "7.2"),
            arrayOf("7.1.0-alpha13", "7.2")
        )

        private fun GradleRunner.appendArguments(vararg arguments: String) =
            withArguments(this.arguments + arguments)

        private fun TemporaryFolder.writeFile(fileName: String, text: () -> String): File {
            val file = File(root, fileName)
            file.parentFile.mkdirs()
            file.writeText(text())
            return file
        }
    }
}
