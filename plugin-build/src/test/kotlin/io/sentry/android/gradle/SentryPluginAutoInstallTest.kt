package io.sentry.android.gradle

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentryPluginAutoInstallTest(
    androidGradlePluginVersion: String,
    gradleVersion: String
) : BaseSentryPluginTest(androidGradlePluginVersion, gradleVersion) {

    @Test
    fun `adds sentry-android dependency`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            sentry {
              includeProguardMapping = false
              autoInstallation.enabled = true
            }
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertTrue {
            "io.sentry:sentry-android:$SENTRY_SDK_VERSION" in result.output
        }
    }

    @Test
    fun `adds integrations`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              // sentry-android shouldn't be installed, since sentry-android-core is present
              implementation 'io.sentry:sentry-android-core:5.1.0'
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'androidx.fragment:fragment:1.3.5'
              // our plugin shouldn't install okhttp, since it's a direct dep
              implementation 'io.sentry:sentry-android-okhttp:5.4.0'
              // our plugin shouldn't install sqlite, since it's a direct dep
              implementation 'io.sentry:sentry-android-sqlite:6.21.0'
            }

            sentry {
              includeProguardMapping = false
              autoInstallation.enabled = true
            }
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertFalse { "io.sentry:sentry-android:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-timber:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-fragment:5.1.0" in result.output }
        assertFalse { "io.sentry:sentry-android-okhttp:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-okhttp:5.4.0" in result.output }
        assertFalse { "io.sentry:sentry-android-sqlite:5.1.0" in result.output }
        assertTrue { "io.sentry:sentry-android-sqlite:6.21.0" in result.output }
        assertFalse { "io.sentry:sentry-compose-android:5.1.0" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `does not do anything when autoinstall is disabled`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'androidx.fragment:fragment:1.3.5'
              implementation 'com.squareup.okhttp3:okhttp:4.9.2'
            }

            sentry.autoInstallation.enabled = false
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertFalse { "io.sentry:sentry-android:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-timber:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-fragment:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-okhttp:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-android-sqlite:$SENTRY_SDK_VERSION" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `uses user-provided sentryVersion when sentry-android is not available in direct deps`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.jakewharton.timber:timber:4.7.1'
              implementation 'com.squareup.okhttp3:okhttp:4.9.2'
              // the fragment integration should stay as it is, the version shouldn't be overridden
              implementation 'io.sentry:sentry-android-fragment:5.4.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "5.1.2"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-android:5.1.2" in result.output }
        assertTrue { "io.sentry:sentry-android-timber:5.1.2" in result.output }
        assertTrue { "io.sentry:sentry-android-okhttp:5.1.2" in result.output }
        assertTrue { "io.sentry:sentry-android-fragment:5.4.0" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `compose is not added for lower sentry versions`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.compose.runtime:runtime:1.1.1'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.6.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-compose-android:6.6.0" in result.output }
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `compose is added with when sentry version 6_7_0 or above is used`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.compose.runtime:runtime:1.1.1'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.7.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-compose-android:6.7.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `sqlite is not added for lower sentry versions`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.sqlite:sqlite:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.20.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-android-sqlite:6.20.0" in result.output }
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `sqlite is added with when sentry version 6_21_0 or above is used`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'androidx.sqlite:sqlite:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.21.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-android-sqlite:6.21.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `logback is added`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'ch.qos.logback:logback-classic:1.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-logback:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `log4j2 is added`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.apache.logging.log4j:log4j-api:2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-log4j2:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for spring-jdbc`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.springframework:spring-jdbc:6.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for hsql`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.hsqldb:hsqldb:2.7.2'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for mysql`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'mysql:mysql-connector-java:8.0.33'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for mariadb`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.mariadb.jdbc:mariadb-java-client:3.1.4'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for postgres`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.postgresql:postgresql:42.6.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for oracle ojdbc5`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.oracle.database.jdbc:ojdbc5:11.2.0.4'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for oracle ojdbc11`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.oracle.database.jdbc:ojdbc11:23.2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for oracle ojdbc8`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'com.oracle.jdbc:ojdbc8:19.3.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `kotlin-extensions is added`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-kotlin-extensions:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring is added for Spring 5`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.springframework:spring-core:5.1.2'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-spring:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-starter:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-jakarta:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-starter-jakarta:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring-jakarta is added for Spring 6`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.springframework:spring-core:6.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-spring:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-starter:6.24.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-jakarta:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-starter-jakarta:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring-boot-starter is added for Spring Boot 2`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.springframework.boot:spring-boot-starter:2.1.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-spring:6.24.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-boot-starter:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-jakarta:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-starter-jakarta:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring-boot-starter-jakarta is added for Spring Boot 3`() {
        appBuildFile.appendText(
            // language=Groovy
            """
            dependencies {
              implementation 'org.springframework.boot:spring-boot-starter:3.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.24.0"
            sentry.includeProguardMapping = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-spring:6.24.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-starter:6.24.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-jakarta:6.24.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-boot-starter-jakarta:6.24.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    private fun runListDependenciesTask() = runner
        .appendArguments("app:dependencies")
        .appendArguments("--configuration")
        .appendArguments("debugRuntimeClasspath")
        .build()
}
