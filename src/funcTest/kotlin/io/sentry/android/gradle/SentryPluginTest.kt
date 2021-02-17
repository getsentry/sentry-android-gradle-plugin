package io.sentry.android.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("FunctionName", "UnusedProperty")
@RunWith(Parameterized::class)
class SentryPluginTest(
    private val androidGradlePluginVersion: String,
    private val gradleVersion: String,
) {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var settingsFile: File
    private lateinit var rootBuildFile: File
    private lateinit var appBuildFile: File
    private lateinit var runner: GradleRunner

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle").apply {
            writeText("include ':app'")
        }
        val pluginClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
            .joinToString(separator = ", ") { "\"$it\"" }

        val releaseProguardRules = "src/release/proguard-rules.pro"

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
                    versionCode 1
                    versionName '0.1'
                    minSdkVersion 21
                    targetSdkVersion 30
                  }
                  
                  buildTypes {
                    release {
                      minifyEnabled true
                      proguardFiles("$releaseProguardRules")
                    }
                  }
                }
              }
              pluginManager.withPlugin('com.android.library') {
                android {
                  compileSdkVersion 30
                  defaultConfig {
                    minSdkVersion 21
                    targetSdkVersion 30
                  }
                  lintOptions {
                    checkReleaseBuilds = false
                  }
                }
              }
            }
            """.trimIndent()
        }

        testProjectDir.newFolder("app")
        appBuildFile = testProjectDir.writeFile("app/build.gradle") {
            // language=Groovy
            """
            plugins {
              id "com.android.application"
              id "io.sentry.android.gradle"
            }
            """.trimIndent()
        }

        testProjectDir.writeFile("app/src/main/AndroidManifest.xml") {
            // language=XML
            """
            <manifest package="com.example" />
            """.trimIndent()
        }

        val configurationFile = testProjectDir.root.resolve("app/build/outputs/mapping/release/configuration.txt")
        testProjectDir.writeFile("app/$releaseProguardRules") {
            //language=PROGUARD
            """
            # This is needed because older versions of AGP did not do this for you
            -printconfiguration $configurationFile
            """.trimIndent()
        }

        testProjectDir.writeFile("sentry.properties") {
            //language=Properties
            """
            defaults.org=example
            defaults.project=example
            auth.token=deadd00d
            """.trimIndent()
        }

        testProjectDir.writeFile("gradle.properties") {
            //language=Properties
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx512m -XX\:MaxMetaspaceSize\=512m
            """.trimIndent()
        }

        runner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--stacktrace", "-Psentry.internal.skipUpload=true")
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
                  // We just want to make sure the extension is registered
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
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }
            """.trimIndent()
        )

        runner
            .appendArguments(":app:assembleRelease")
            .build()

        verifyProguardUuid()
    }

    @Test
    fun `generates proguard files when enabled`() {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }
                
                sentry {
                  autoProguardConfig = true
                }
            """.trimIndent()
        )

        runner
            .appendArguments(":app:assembleRelease")
            .build()

        val generatedMappings = testProjectDir.root.resolve("app/build/intermediates/sentry/sentry.pro")
            .readText()
        val finalMappings = testProjectDir.root.resolve("app/build/outputs/mapping/release/configuration.txt")
            .readText()

        assertTrue(generatedMappings in finalMappings)
    }

    @Test
    fun `skips proguard config when disabled`() {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }
                
                sentry {
                  autoProguardConfig = false
                }
            """.trimIndent()
        )

        runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertFalse(testProjectDir.root.resolve("app/build/intermediates/sentry/sentry.pro").exists())
        val finalMappings = testProjectDir.root.resolve("app/build/outputs/mapping/release/configuration.txt")
            .readText()

        assertFalse("sentry.pro" in finalMappings)
    }

    @Test
    fun `proguard config task is cached`() {
        runner.appendArguments(":app:addSentryProguardSettings")

        runner.build()
        val result = runner.build()
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":app:addSentryProguardSettings")?.outcome)
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
        private val assetPattern = Regex("""^io\.sentry\.ProguardUuids=([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$""".trimMargin())

        @OptIn(ExperimentalStdlibApi::class)
        @Parameterized.Parameters(name = "AGP {0}, Gradle {1}")
        @JvmStatic
        fun parameters(): Collection<*> = buildSet {
            // The supported Gradle version can be found here:
            // https://developer.android.com/studio/releases/gradle-plugin#updating-gradle
            // The pair is [AGP Version, Gradle Version]
            add(arrayOf("3.4.3", "6.1.1"))
            add(arrayOf("3.5.4", "6.1.1"))
            add(arrayOf("3.6.4", "6.1.1"))
            add(arrayOf("4.0.0", "6.1.1"))
            add(arrayOf("4.1.2", "6.5"))
            add(arrayOf("4.1.2", "6.8.1"))
            add(arrayOf("4.2.0-beta04", "6.8.1"))
        }

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