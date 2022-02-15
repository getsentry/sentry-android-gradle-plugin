package io.sentry.android.gradle

import io.sentry.android.gradle.extensions.InstrumentationFeature
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class SentryPluginTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

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
                  autoUploadProguardMapping = false
                  tracingInstrumentation {
                    enabled = false
                  }
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
            .toMutableList()

        // AGP 7.2.x configures the 'clean' task, so ignore it
        configuredTasks.remove(":app:clean")

        assertTrue(configuredTasks.isEmpty())
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
    fun `does not include a UUID in the APK if includeProguardMapping is off`() {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                sentry {
                  includeProguardMapping = false
                }
            """.trimIndent()
        )

        runner
            .appendArguments(":app:assembleRelease")
            .build()

        assertThrows(AssertionError::class.java) {
            verifyProguardUuid(testProjectDir.root)
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

    @Test
    fun `applies only DB instrumentables when only DATABASE feature enabled`() {
        applyTracingInstrumentation(features = setOf(InstrumentationFeature.DATABASE))

        val build = runner
            .appendArguments(":app:assembleDebug", "--debug")
            .build()

        assertTrue {
            "[sentry] Instrumentables: AndroidXSQLiteDatabase, AndroidXSQLiteStatement," +
                " AndroidXRoomDao" in build.output
        }
    }

    @Test
    fun `applies only FILE_IO instrumentables when only FILE_IO feature enabled`() {
        applyTracingInstrumentation(features = setOf(InstrumentationFeature.FILE_IO))

        val build = runner
            .appendArguments(":app:assembleDebug", "--debug")
            .build()

        assertTrue {
            "[sentry] Instrumentables: ChainedInstrumentable" in build.output
        }
    }

    @Test
    fun `applies all instrumentables when all features enabled`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.DATABASE, InstrumentationFeature.FILE_IO)
        )

        val build = runner
            .appendArguments(":app:assembleDebug", "--debug")
            .build()

        assertTrue {
            "[sentry] Instrumentables: AndroidXSQLiteDatabase, AndroidXSQLiteStatement," +
                " AndroidXRoomDao, ChainedInstrumentable" in build.output
        }
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
                  autoUploadProguardMapping = false
                  uploadNativeSymbols = true
                  tracingInstrumentation {
                    enabled = false
                  }
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
                  autoUploadProguardMapping = true
                  ignoredVariants = ["$ignoredVariant"]
                  tracingInstrumentation {
                    enabled = false
                  }
                }
            """.trimIndent()
        )
    }

    private fun applyTracingInstrumentation(
        tracingInstrumentation: Boolean = true,
        features: Set<InstrumentationFeature> = setOf()
    ) {
        appBuildFile.writeText(
            // language=Groovy
            """
                plugins {
                  id "com.android.application"
                  id "io.sentry.android.gradle"
                }

                dependencies {
                  implementation 'io.sentry:sentry-android:5.5.0'
                }

                sentry {
                  autoUploadProguardMapping = false
                  tracingInstrumentation {
                    enabled = $tracingInstrumentation
                    features = ${
            features.joinToString(
                prefix = "[",
                postfix = "]"
            ) { "${it::class.java.canonicalName}.${it.name}" }
            }
                  }
                }
            """.trimIndent()
        )
    }
}
