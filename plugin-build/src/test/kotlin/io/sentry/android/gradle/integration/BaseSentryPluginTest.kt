package io.sentry.android.gradle.integration

import io.sentry.android.gradle.util.PrintBuildOutputOnFailureRule
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.gradle.testkit.runner.internal.io.SynchronizedOutputStream
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName

@Suppress("FunctionName")
abstract class BaseSentryPluginTest(
    protected val androidGradlePluginVersion: String,
    private val gradleVersion: String
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    @get:Rule
    val name = TestName()

    private val outputStream = ByteArrayOutputStream()
    private val writer = OutputStreamWriter(SynchronizedOutputStream(outputStream))

    @get:Rule
    val printBuildOutputOnFailureRule = PrintBuildOutputOnFailureRule(outputStream)

    private val projectTemplateFolder = File("src/test/resources/testFixtures/appTestProject")
    private val mavenTestRepoPath = File("./../build/mavenTestRepo")

    private lateinit var rootBuildFile: File
    protected lateinit var appBuildFile: File
    protected lateinit var moduleBuildFile: File
    protected lateinit var sentryPropertiesFile: File
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
        sentryPropertiesFile = File(testProjectDir.root, "sentry.properties")
        rootBuildFile = testProjectDir.writeFile("build.gradle") {
            // language=Groovy
            """
            import io.sentry.android.gradle.autoinstall.AutoInstallState
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

              pluginManager.withPlugin('io.sentry.android.gradle') {
                tasks.register('cleanupAutoInstallState') {
                  doLast {
                    AutoInstallState.clearReference()
                  }
                }
              }
            }
            """.trimIndent()
        }

        runner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--stacktrace", "-DtestName=${name.methodName}")
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
//            .withDebug(true)
            .forwardStdOutput(writer)
            .forwardStdError(writer)
    }

    @After
    fun teardown() {
        runner.appendArguments("app:cleanupAutoInstallState").build()
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
