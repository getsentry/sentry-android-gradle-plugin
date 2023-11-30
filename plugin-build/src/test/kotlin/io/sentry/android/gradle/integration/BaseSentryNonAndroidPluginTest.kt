package io.sentry.android.gradle.integration

import io.sentry.android.gradle.util.PrintBuildOutputOnFailureRule
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.gradle.testkit.runner.internal.io.SynchronizedOutputStream
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class BaseSentryNonAndroidPluginTest(
    private val gradleVersion: String
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val outputStream = ByteArrayOutputStream()
    private val writer = OutputStreamWriter(SynchronizedOutputStream(outputStream))

    @get:Rule
    val printBuildOutputOnFailureRule = PrintBuildOutputOnFailureRule(outputStream)

    private val projectTemplateFolder = File("src/test/resources/testFixtures/appTestProject")

    protected lateinit var root: File
    private lateinit var rootBuildFile: File
    protected lateinit var appBuildFile: File
    protected lateinit var moduleBuildFile: File
    protected lateinit var sentryPropertiesFile: File
    protected lateinit var runner: GradleRunner

    protected open val additionalRootProjectConfig: String = ""
    protected open val additionalBuildClasspath: String = ""

    @Before
    fun setup() {
        root = File(
            testProjectDir.root.absolutePath,
            "gradle-$gradleVersion"
        ).also { it.mkdirs() }
        projectTemplateFolder.copyRecursively(root)

        val pluginClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
            .joinToString(separator = ", ") { "\"$it\"" }
            .replace(File.separator, "/")

        appBuildFile = File(root, "app/build.gradle")
        moduleBuildFile = File(root, "module/build.gradle")
        sentryPropertiesFile = File(root, "sentry.properties")
        rootBuildFile = testProjectDir.writeFile(
            "${root.relativeTo(testProjectDir.root)}${File.separator}build.gradle"
        ) {
            // language=Groovy
            """
            buildscript {
              repositories {
                google()
                gradlePluginPortal()
                mavenCentral()
              }
              dependencies {
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0'
                // This is needed to populate the plugin classpath instead of using
                // withPluginClasspath on the Gradle Runner.
                $additionalBuildClasspath
                classpath files($pluginClasspath)
              }
            }

            allprojects {
              repositories {
                google()
                mavenCentral()
                mavenLocal()
              }
            }
            """.trimIndent()
        }

        runner = GradleRunner.create()
            .withProjectDir(root)
            .withArguments("--stacktrace")
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .forwardStdOutput(writer)
            .forwardStdError(writer)
    }

    companion object {

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
