package io.sentry.android.gradle.integration

import io.sentry.BuildConfig
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.verifyDependenciesReportAndroid
import io.sentry.android.gradle.verifyIntegrationList
import io.sentry.android.gradle.verifyProguardUuid
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeThat
import org.junit.Test

class SentryPluginTest :
    BaseSentryPluginTest(BuildConfig.AgpVersion, GradleVersion.current().version) {

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
        applyTracingInstrumentation(
            false,
            appStart = false,
            logcat = false
        )

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
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.DATABASE),
            dependencies = setOf(
                "androidx.sqlite:sqlite:2.0.0",
                "io.sentry:sentry-android-sqlite:6.21.0"
            ),
            appStart = false,
            logcat = false
        )

        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "AndroidXSQLiteOpenHelper, AndroidXRoomDao)" in build.output
        }
    }

    @Test
    fun `applies only FILE_IO instrumentables when only FILE_IO feature enabled`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.FILE_IO),
            appStart = false,
            logcat = false
        )

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
                "androidx.compose.runtime:runtime:1.1.0",
                "io.sentry:sentry-compose-android:6.7.0"
            ),
            appStart = false,
            logcat = false
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
            features = setOf(InstrumentationFeature.COMPOSE),
            appStart = false,
            logcat = false
        )

        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()
        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=)" in build.output
        }
    }

    @Test
    fun `apply old Database instrumentable when app does not depend on sentry-android-sqlite`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.DATABASE),
            appStart = false,
            logcat = false
        )

        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()
        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "AndroidXSQLiteDatabase, AndroidXSQLiteStatement, AndroidXRoomDao)" in build.output
        }
    }

    @Test
    fun `does not apply okhttp listener on older version of sentry-android-okhttp`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.OKHTTP),
            dependencies = setOf(
                "com.squareup.okhttp3:okhttp:3.14.9",
                "io.sentry:sentry-android-okhttp:6.19.0"
            ),
            appStart = false,
            logcat = false
        )
        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=OkHttp)" in build.output
        }
    }

    @Test
    fun `apply okhttp listener on sentry-android-okhttp 6_20`() {
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.OKHTTP),
            dependencies = setOf(
                "com.squareup.okhttp3:okhttp:3.14.9",
                "io.sentry:sentry-android-okhttp:6.20.0"
            ),
            appStart = false,
            logcat = false
        )
        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "OkHttpEventListener, OkHttp)" in build.output
        }
    }

    @Test
    fun `applies app start instrumentations when enabled`() {
        applyTracingInstrumentation(
            appStart = true
        )
        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "Application, ContentProvider)" in build.output
        }
    }

    @Test
    fun `applies logcat instrumentation when enabled`() {
        applyTracingInstrumentation(
            appStart = false,
            logcat = true
        )
        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "Logcat)" in build.output
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
                "androidx.sqlite:sqlite:2.0.0",
                "com.squareup.okhttp3:okhttp:3.14.9",
                "io.sentry:sentry-android-okhttp:6.20.0",
                "androidx.compose.runtime:runtime:1.1.0",
                "io.sentry:sentry-compose-android:6.7.0",
                "io.sentry:sentry-android-sqlite:6.21.0"
            ),
            appStart = true,
            logcat = true
        )
        val build = runner
            .appendArguments(":app:assembleDebug", "--info")
            .build()

        assertTrue {
            "[sentry] Instrumentable: ChainedInstrumentable(instrumentables=" +
                "AndroidXSQLiteOpenHelper, AndroidXRoomDao, OkHttpEventListener, " +
                "OkHttp, WrappingInstrumentable, RemappingInstrumentable, " +
                "ComposeNavigation, Logcat, Application, ContentProvider)" in build.output
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

    @Test
    fun `includes flattened list of dependencies into the APK, excluding non-external deps`() {
        appBuildFile.appendText(
            // language=Groovy
            """

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
              implementation project(':module') // multi-module project dependency
              implementation ':asm-9.2' // flat jar
            }
            """.trimIndent()
        )
        runner.appendArguments(":app:assembleDebug")

        runner.build()
        val deps = verifyDependenciesReportAndroid(testProjectDir.root)
        assertEquals(
            """
            com.squareup.okhttp3:okhttp:3.14.9
            com.squareup.okio:okio:1.17.2
            """.trimIndent(),
            deps
        )
    }

    @Test
    fun `tracks dependency tree changed`() {
        appBuildFile.appendText(
            // language=Groovy
            """

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
            }
            """.trimIndent()
        )
        runner.appendArguments(":app:assembleDebug")

        runner.build()
        val deps = verifyDependenciesReportAndroid(testProjectDir.root)
        assertEquals(
            """
            com.squareup.okhttp3:okhttp:3.14.9
            com.squareup.okio:okio:1.17.2
            """.trimIndent(),
            deps
        )

        appBuildFile.appendText(
            // language=Groovy
            """

            dependencies {
              implementation 'com.jakewharton.timber:timber:5.0.1'
            }
            """.trimIndent()
        )
        runner.build()
        val depsAfterChange = verifyDependenciesReportAndroid(testProjectDir.root)
        assertEquals(
            """
            com.jakewharton.timber:timber:5.0.1
            com.squareup.okhttp3:okhttp:3.14.9
            com.squareup.okio:okio:1.17.2
            org.jetbrains.kotlin:kotlin-stdlib-common:1.5.21
            org.jetbrains.kotlin:kotlin-stdlib:1.5.21
            org.jetbrains:annotations:20.1.0
            """.trimIndent(),
            depsAfterChange
        )
    }

    @Test
    fun `when disabled, skips the task and does not include dependencies report in the APK`() {
        appBuildFile.appendText(
            // language=Groovy
            """

            dependencies {
              implementation 'com.squareup.okhttp3:okhttp:3.14.9'
            }

            sentry.includeDependenciesReport = false
            """.trimIndent()
        )
        val output = runner
            .appendArguments(":app:assembleDebug")
            .build()
            .output

        assertTrue { "collectExternalDebugDependenciesForSentry" !in output }
        assertThrows(AssertionError::class.java) {
            verifyDependenciesReportAndroid(testProjectDir.root)
        }
    }

    @Test
    fun `all integrations are written to manifest`() {
        applyTracingInstrumentation(
            features = setOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.OKHTTP,
                InstrumentationFeature.COMPOSE
            ),
            logcat = true,
            appStart = true,
            dependencies = setOf(
                "com.squareup.okhttp3:okhttp:3.14.9",
                "io.sentry:sentry-android-okhttp:6.6.0",
                "androidx.compose.runtime:runtime:1.1.0",
                "io.sentry:sentry-compose-android:6.7.0"
            )
        )

        runner.appendArguments(":app:assembleDebug")
        runner.build()

        val integrations = verifyIntegrationList(
            testProjectDir.root,
            variant = "debug",
            signed = false
        ).sorted()

        val expectedIntegrations = (
            listOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.COMPOSE,
                InstrumentationFeature.OKHTTP
            ).map { it.integrationName }.toMutableList() +
                listOf("LogcatInstrumentation", "AppStartInstrumentation")
            )
            .sorted()

        assertEquals(expectedIntegrations, integrations)
    }

    @Test
    fun `only active integrations are written to manifest`() {
        applyTracingInstrumentation(
            features = setOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.OKHTTP,
                InstrumentationFeature.COMPOSE
            ),
            dependencies = setOf(
                "androidx.compose.runtime:runtime:1.1.0",
                "io.sentry:sentry-compose-android:6.7.0"
            )
        )

        runner.appendArguments(":app:assembleDebug")

        runner.build()
        val integrations = verifyIntegrationList(
            testProjectDir.root,
            variant = "debug",
            signed = false
        ).sorted()

        val expectedIntegrations = listOf(
            InstrumentationFeature.DATABASE,
            InstrumentationFeature.COMPOSE,
            InstrumentationFeature.FILE_IO
        ).map { it.integrationName }.sorted()

        assertEquals(expectedIntegrations, integrations)
    }

    @Test
    fun `no integrations are written to manifest if not configured`() {
        applyTracingInstrumentation(
            dependencies = setOf(
                "com.squareup.okhttp3:okhttp:3.14.9",
                "androidx.compose.runtime:runtime:1.1.0",
                "io.sentry:sentry-compose-android:6.7.0"
            )
        )

        runner.appendArguments(":app:assembleDebug")

        runner.build()

        assertThrows(NoSuchElementException::class.java) {
            verifyIntegrationList(testProjectDir.root, variant = "debug", signed = false)
        }
    }

    @Test
    fun `no integrations are written to manifest if instrumentation is disabled`() {
        applyTracingInstrumentation(
            tracingInstrumentation = false,
            features = setOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.OKHTTP,
                InstrumentationFeature.COMPOSE
            ),
        )

        runner.appendArguments(":app:assembleRelease")

        runner.build()

        assertThrows(NoSuchElementException::class.java) {
            verifyIntegrationList(testProjectDir.root)
        }
    }

    @Test
    fun `does not instrument classes that are provided in excludes`() {
        assumeThat(
            "We only support the 'excludes' option from AGP 7.4.0 onwards",
            SemVer.parse(androidGradlePluginVersion) >= AgpVersions.VERSION_7_4_0,
            `is`(true)
        )
        applyTracingInstrumentation(
            features = setOf(InstrumentationFeature.OKHTTP),
            debug = true,
            excludes = setOf("okhttp3/RealCall")
        )
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

        // since it's an integration test, we just test that the log file wasn't created
        // for the class meaning our CommonClassVisitor has NOT instrumented it
        val debugOutput =
            testProjectDir.root.resolve("app/build/tmp/sentry/RealCall-instrumentation.log")
        assertTrue { !debugOutput.exists() }
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
        logcat: Boolean = false,
        appStart: Boolean = false,
        dependencies: Set<String> = emptySet(),
        debug: Boolean = false,
        excludes: Set<String> = emptySet()
    ) {
        appBuildFile.appendText(
            // language=Groovy
            """
                dependencies {
                  implementation 'io.sentry:sentry-android:7.1.0'
                  ${dependencies.joinToString("\n") { "implementation '$it'" }}
                }

                sentry {
                  autoUploadProguardMapping = false
                  tracingInstrumentation {
                    forceInstrumentDependencies = true
                    enabled = $tracingInstrumentation
                    debug = $debug
                    features = [${features.joinToString { "${it::class.java.canonicalName}.${it.name}" }}]
                    appStart {
                        enabled = $appStart
                    }
                    logcat {
                        enabled = $logcat
                    }
                    excludes = ["${excludes.joinToString()}"]
                  }
                }
            """.trimIndent()
        )
    }
}
