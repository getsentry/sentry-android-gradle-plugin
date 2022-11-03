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
        appBuildFile.appendText(
            // language=Groovy
            """
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
        appBuildFile.appendText(
            // language=Groovy
            """
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
        appBuildFile.appendText(
            // language=Groovy
            """
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
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "AndroidXSQLiteDatabase, AndroidXSQLiteStatement, AndroidXRoomDao)" in build.output
        }
    }

    @Test
    fun `applies only FILE_IO instrumentables when only FILE_IO feature enabled`() {
        applyTracingInstrumentation(features = setOf(InstrumentationFeature.FILE_IO))

        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "WrappingInstrumentable, RemappingInstrumentable)" in build.output
        }
    }

    @Test
    fun `applies only Compose instrumentable when only Compose feature enabled`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.COMPOSE),
            dependencies = setOf(
                "androidx.compose.runtime:runtime:1.1.0"
            )
        )

        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()
        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "ComposeNavigation)" in build.output
        }
    }

    @Test
    fun `does not apply Compose instrumentable when app does not depend on compose (runtime)`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.COMPOSE)
        )

        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()
        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=)" in build.output
        }
    }

    @Test
    fun `applies all instrumentables when all features are enabled`() {
        applyTracingInstrumentation(
            features = setOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.OKHTTP,
                InstrumentationFeature.COMPOSE
            ),
            dependencies = setOf(
                "com.squareup.okhttp3:okhttp:3.14.9",
                "io.sentry:sentry-android-okhttp:6.6.0",
                "androidx.compose.runtime:runtime:1.0.0"
            )
        )
        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "AndroidXSQLiteDatabase, AndroidXSQLiteStatement, AndroidXRoomDao, OkHttp, " +
                "WrappingInstrumentable, RemappingInstrumentable, " +
                "ComposeNavigation)" in build.output
        }
    }

    @Test
    fun `instruments okhttp v3`() {
        applyTracingInstrumentation(features = setOf(InstrumentationFeature.OKHTTP), debug = true)
        appBuildFile.appendText(
            // language=Groovy
            """

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
              implementation 'io.sentry:sentry-android-okhttp:6.6.0'
            }
            """.trimIndent()
        )

        runner
            .appendArguments(":app:assembleDebug")
            .build()

        // since it's an integration test, we just test that the log file was created for the class
        // meaning our CommonClassVisitor has visited and instrumented it
        val debugOutput =
            testProjectDir.root.resolve("app/build/tmp/sentry/RealCall-instrumentation.log")
        assertTrue { debugOutput.exists() && debugOutput.length() > 0 }
    }

    private fun applyUploadNativeSymbols() {
        appBuildFile.appendText(
            // language=Groovy
            """
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
        appBuildFile.appendText(
            // language=Groovy
            """
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
        features: Set<InstrumentationFeature> = emptySet(),
        dependencies: Set<String> = emptySet(),
        debug: Boolean = false
    ) {
        appBuildFile.appendText(
            // language=Groovy
            """
                dependencies {
                  implementation 'io.sentry:sentry-android:6.6.0'
                  ${dependencies.joinToString("\n") { "implementation '$it'" }}
                }

                sentry {
                  autoUploadProguardMapping = false
                  tracingInstrumentation {
                    forceInstrumentDependencies = true
                    enabled = $tracingInstrumentation
                    debug = $debug
                    features = [${features.joinToString { "${it::class.java.canonicalName}.${it.name}" }}]
                  }
                }
            """.trimIndent()
        )
    }
}
