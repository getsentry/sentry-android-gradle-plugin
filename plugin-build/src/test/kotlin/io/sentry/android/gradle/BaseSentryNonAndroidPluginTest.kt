package io.sentry.android.gradle

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runners.Parameterized

@Suppress("FunctionName")
abstract class BaseSentryNonAndroidPluginTest(
    private val gradleVersion: String
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val projectTemplateFolder = File("src/test/resources/testFixtures/appTestProject")
    private val mavenTestRepoPath = File("./../build/mavenTestRepo")

    private lateinit var rootBuildFile: File
    protected lateinit var appBuildFile: File
    protected lateinit var moduleBuildFile: File
    protected lateinit var runner: GradleRunner

    protected open val additionalRootProjectConfig: String = ""
    protected open val additionalBuildClasspath: String = ""

    @Before
    fun setup() {
        projectTemplateFolder.copyRecursively(testProjectDir.root)

        val pluginClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
            .joinToString(separator = ", ") { "\"$it\"" }
            .replace(File.separator, "/")

        appBuildFile = File(testProjectDir.root, "app/build.gradle")
        moduleBuildFile = File(testProjectDir.root, "module/build.gradle")
        rootBuildFile = testProjectDir.writeFile("build.gradle") {
            // language=Groovy
            """
            buildscript {
              repositories {
                google()
                gradlePluginPortal()
                mavenCentral()
              }
              dependencies {
                // This is needed to populate the plugin classpath instead of using
                // withPluginClasspath on the Gradle Runner.
                $additionalBuildClasspath
                classpath files($pluginClasspath)
              }
            }

            allprojects {
              repositories {
                maven {
                  url = "${mavenTestRepoPath.absoluteFile.toURI()}"
                }
                google()
                mavenCentral()
                mavenLocal()
                maven { url 'https://appboy.github.io/appboy-android-sdk/sdk' }
                maven { url 'https://pkgs.dev.azure.com/Synerise/AndroidSDK/_packaging/prod/maven/v1' }
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

    companion object {

        @Parameterized.Parameters(name = "Gradle {0}")
        @JvmStatic
        fun parameters() = listOf(
            // The supported Gradle version can be found here:
            // https://developer.android.com/studio/releases/gradle-plugin#updating-gradle
            // The pair is [AGP Version, Gradle Version]
            arrayOf("7.2"),
            arrayOf("7.3.3"),
            arrayOf("7.4"),
            arrayOf("7.5"),
            arrayOf("7.6"),
            arrayOf("8.0.2"),
            arrayOf("8.1")
        )

        internal fun GradleRunner.appendArguments(vararg arguments: String) =
            withArguments(this.arguments + arguments)

        private fun TemporaryFolder.writeFile(fileName: String, text: () -> String): File {
            val file = File(root, fileName)
            file.parentFile.mkdirs()
            file.writeText(text())
            return file
        }
    }
}
