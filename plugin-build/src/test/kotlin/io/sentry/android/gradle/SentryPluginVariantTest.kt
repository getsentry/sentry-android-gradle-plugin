package io.sentry.android.gradle

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginVariantTest(
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
                  flavorDimensions "version"
                  productFlavors {
                    create("demo") {
                        applicationIdSuffix = ".demo"
                    }
                    create("full") {
                        applicationIdSuffix = ".full"
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
    fun `skips variant if set with ignoredVariants`() {
        applyIgnores(ignoredVariants = listOf("fullRelease"))

        val build = runner
            .appendArguments(":app:assembleFullRelease", "--dry-run")
            .build()

        assertFalse(":app:uploadSentryProguardMappingsFullRelease" in build.output)
    }

    @Test
    fun `does not skip variant if not included in ignoredVariants`() {
        applyIgnores(ignoredVariants = listOf("demoRelease", "fullDebug", "demoDebug"))

        val build = runner
            .appendArguments(":app:assembleFullRelease", "--dry-run")
            .build()

        assertTrue(":app:uploadSentryProguardMappingsFullRelease" in build.output)
    }

    @Test
    fun `skips buildType if set with ignoredBuildTypes`() {
        applyIgnores(ignoredBuildTypes = listOf("debug"))

        val build = runner
            .appendArguments(":app:assembleFullDebug", "--dry-run")
            .build()

        assertFalse(":app:uploadSentryProguardMappingsFullDebug" in build.output)
        assertFalse(":app:uploadSentryProguardMappingsDemoDebug" in build.output)
    }

    @Test
    fun `does not skip buildType if not included in ignoredBuildTypes`() {
        applyIgnores(ignoredBuildTypes = listOf("debug"))

        val build = runner
            .appendArguments(":app:assembleFullRelease", "--dry-run")
            .build()

        assertTrue(":app:uploadSentryProguardMappingsFullRelease" in build.output)
    }

    @Test
    fun `skips flavor if set with ignoredFlavors`() {
        applyIgnores(ignoredFlavors = listOf("full"))

        var build = runner
            .appendArguments(":app:assembleFullDebug", "--dry-run")
            .build()

        assertFalse(":app:uploadSentryProguardMappingsFullDebug" in build.output)

        build = runner
            .appendArguments(":app:assembleFullRelease", "--dry-run")
            .build()

        assertFalse(":app:uploadSentryProguardMappingsFullRelease" in build.output)
    }

    @Test
    fun `does not skip flavor if not included in ignoredFlavors`() {
        applyIgnores(ignoredFlavors = listOf("full"))

        val build = runner
            .appendArguments(":app:assembleDemoRelease", "--dry-run")
            .build()

        assertTrue(":app:uploadSentryProguardMappingsDemoRelease" in build.output)
    }

    private fun applyIgnores(
        ignoredVariants: List<String> = listOf(),
        ignoredBuildTypes: List<String> = listOf(),
        ignoredFlavors: List<String> = listOf()
    ) {
        val variants = ignoredVariants.joinToString(",") { "\"$it\"" }
        val buildTypes = ignoredBuildTypes.joinToString(",") { "\"$it\"" }
        val flavors = ignoredFlavors.joinToString(",") { "\"$it\"" }
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                sentry {
                  autoUpload = true
                  ignoredVariants = [$variants]
                  ignoredBuildTypes = [$buildTypes]
                  ignoredFlavors = [$flavors]
                  tracingInstrumentation {
                    enabled = false
                  }
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
            arrayOf("7.0.3", "7.0.2"),
            arrayOf("7.0.3", "7.1.1"),
            arrayOf("7.0.3", "7.2"),
            arrayOf("7.1.0-beta01", "7.2"),
            arrayOf("7.2.0-alpha01", "7.2")
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
