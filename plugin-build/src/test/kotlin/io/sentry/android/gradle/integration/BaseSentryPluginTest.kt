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

@Suppress("FunctionName")
abstract class BaseSentryPluginTest(
    protected val androidGradlePluginVersion: String,
    private val gradleVersion: String
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val outputStream = ByteArrayOutputStream()
    private val writer = OutputStreamWriter(SynchronizedOutputStream(outputStream))

    @get:Rule
    val printBuildOutputOnFailureRule = PrintBuildOutputOnFailureRule(outputStream)

    private val projectTemplateFolder = File("src/test/resources/testFixtures/appTestProject")
    private val mavenTestRepoPath = File("./../build/mavenTestRepo")

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
            "gradle-$gradleVersion${File.separator}agp-$androidGradlePluginVersion"
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
                classpath 'com.android.tools.build:gradle:$androidGradlePluginVersion'
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
            subprojects {
              pluginManager.withPlugin('com.android.application') {
                android {
                  compileSdkVersion 33
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
                  $additionalRootProjectConfig
                }
              }
            }
            """.trimIndent()
        }

        runner = GradleRunner.create()
            .withProjectDir(root)
            .withArguments("--stacktrace")
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
//            .withDebug(true)
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
