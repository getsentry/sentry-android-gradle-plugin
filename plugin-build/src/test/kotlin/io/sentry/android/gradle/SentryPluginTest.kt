package io.sentry.android.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginTest(
    private val androidGradlePluginVersion: String,
    private val gradleVersion: String,
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    val projectTemplateFolder = File("src/test/resources/testFixtures/appTestProject")

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
                jcenter()
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
    fun `regenerates UUID every build`() {
        runner.appendArguments(":app:assembleRelease")

        runner.build()
        val uuid1 = verifyProguardUuid()

        runner.build()
        val uuid2 = verifyProguardUuid()

        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun `includes a UUID in the APK`() {
        runner
            .appendArguments(":app:assembleRelease")
            .build()

        verifyProguardUuid()
    }

    private fun verifyProguardUuid(variant: String = "release"): UUID {
        val apk = testProjectDir.root.resolve("app/build/outputs/apk/$variant/app-$variant-unsigned.apk")
        val sentryProperties = with(ZipFile(apk)) {
            val entry = getEntry("assets/sentry-debug-meta.properties")
            assertNotNull(entry, "Asset not included")
            getInputStream(entry).bufferedReader().use {
                it.readText()
            }
        }
        val matcher = assetPattern.matchEntire(sentryProperties)
        assertNotNull(matcher, "$sentryProperties does not match pattern $assetPattern")
        return UUID.fromString(matcher.groupValues[1])
    }

    companion object {
        private val assetPattern =
            Regex("""^io\.sentry\.ProguardUuids=([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$""".trimMargin())

        @Parameterized.Parameters(name = "AGP {0}, Gradle {1}")
        @JvmStatic
        fun parameters() = listOf(
            // The supported Gradle version can be found here:
            // https://developer.android.com/studio/releases/gradle-plugin#updating-gradle
            // The pair is [AGP Version, Gradle Version]
            arrayOf("3.4.3", "6.1.1"),
            arrayOf("3.5.4", "6.1.1"),
            arrayOf("3.6.4", "6.1.1"),
            arrayOf("4.0.0", "6.1.1"),
            arrayOf("4.1.2", "6.5"),
            arrayOf("4.1.2", "6.8.1"),
            arrayOf("4.2.0-beta04", "6.8.1"),
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
