package io.sentry.android.gradle.integration

import io.sentry.android.gradle.integration.BaseSentryPluginTest.Companion.appendArguments
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

            subprojects {
              pluginManager.withPlugin('io.sentry.jvm.gradle') {
                tasks.register('cleanupAutoInstallState') {
                  doLast {
                    AutoInstallState.clearReference()
                  }
                }
                tasks.register('unlockTransforms', Exec) {
                  commandLine 'find', project.gradle.gradleUserHomeDir, '-type', 'f', '-name', 'transforms-3.lock', '-delete'
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
            .forwardStdOutput(writer)
            .forwardStdError(writer)

        runner.appendArguments("app:unlockTransforms").build()
    }

    @After
    fun teardown() {
        try {
            runner.appendArguments("app:cleanupAutoInstallState").build()
        } catch (ignored: Throwable) {
            // may fail if we are relying on BuildFinishesListener, but we don't care here
        }
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
