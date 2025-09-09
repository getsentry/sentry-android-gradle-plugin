package io.sentry.android.gradle.integration

import io.sentry.android.gradle.util.PrintBuildOutputOnFailureRule
import io.sentry.android.gradle.util.SemVer
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

@Suppress("FunctionName")
abstract class BaseSentryPluginTest(
  protected val androidGradlePluginVersion: String,
  private val gradleVersion: String,
) {
  @get:Rule val testProjectDir = TemporaryFolder()

  private val outputStream = ByteArrayOutputStream()
  private val writer = OutputStreamWriter(SynchronizedOutputStream(outputStream))

  @get:Rule val printBuildOutputOnFailureRule = PrintBuildOutputOnFailureRule(outputStream)

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

    val pluginClasspath =
      PluginUnderTestMetadataReading.readImplementationClasspath()
        .joinToString(separator = ", ") { "\"$it\"" }
        .replace(File.separator, "/")

    // AGP 7.x does not work well with SDK 34+ (some R8-related shenanigans)
    val compileSdkVersion =
      if (SemVer.parse(androidGradlePluginVersion) < SemVer.parse("8.0.0")) 33 else 34
    appBuildFile = File(testProjectDir.root, "app/build.gradle")
    moduleBuildFile = File(testProjectDir.root, "module/build.gradle")
    sentryPropertiesFile = File(testProjectDir.root, "sentry.properties")
    rootBuildFile =
      testProjectDir.writeFile("build.gradle") {
        // language=Groovy
        """
            import io.sentry.android.gradle.autoinstall.AutoInstallState
            import io.sentry.android.gradle.util.GradleVersions

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
                  compileSdkVersion $compileSdkVersion
                  defaultConfig {
                    applicationId "com.example"
                    minSdkVersion 21
                  }
                  buildTypes {
                    release {
                      minifyEnabled true
                      proguardFiles("proguard-rules.pro")
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
            """
          .trimIndent()
      }

    runner =
      GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("--stacktrace")
        .withPluginClasspath()
        .withGradleVersion(gradleVersion)
        //            .withDebug(true)
        .forwardStdOutput(writer)
        .forwardStdError(writer)

    unlockTransforms()
  }

  private fun unlockTransforms() {
    val gradleUserHome = File("build/tmp/integrationTest/work/.gradle-test-kit").absolutePath

    val command =
      listOf("find", gradleUserHome, "-type", "f", "-name", "transforms-3.lock", "-delete")

    try {
      val process = ProcessBuilder(command).redirectErrorStream(true).start()

      val output = process.inputStream.bufferedReader().readText()
      val exitCode = process.waitFor()

      if (exitCode != 0) {
        println(output)
        System.err.println("Unlock failed with exit code: $exitCode")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @After
  fun teardown() {
    try {
      runner.withArguments("app:cleanupAutoInstallState").build()
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
