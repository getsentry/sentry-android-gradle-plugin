package io.sentry.android.gradle.integration

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Test

class SentryPluginAutoInstallNonAndroidTest :
    BaseSentryNonAndroidPluginTest(GradleVersion.current().version) {

    @Test
    fun `adds sentry dependency`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }

            sentry {
              autoInstallation.enabled = true
            }
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertTrue {
            "io.sentry:sentry:$SENTRY_SDK_VERSION" in result.output
        }
    }

    @Test
    fun `does not do anything when autoinstall is disabled`() {
        assumeThat(getJavaVersion() >= 17, `is`(true))
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }

            dependencies {
              implementation 'org.springframework.boot:spring-boot-starter:3.0.0'
              implementation 'ch.qos.logback:logback-classic:1.0.0'
              implementation 'org.apache.logging.log4j:log4j-api:2.0'
            }

            sentry.autoInstallation.enabled = false
            """.trimIndent()
        )

        val result = runListDependenciesTask()
        assertFalse { "io.sentry:sentry:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-spring:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-spring-jakarta:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot:$SENTRY_SDK_VERSION" in result.output }
        assertFalse {
            "io.sentry:sentry-spring-boot-jakarta:$SENTRY_SDK_VERSION" in result.output
        }
        assertFalse { "io.sentry:sentry-logback:$SENTRY_SDK_VERSION" in result.output }
        assertFalse { "io.sentry:sentry-log4j2:$SENTRY_SDK_VERSION" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `uses user-provided sentryVersion when sentry is not available in direct deps`() {
        assumeThat(getJavaVersion() >= 17, `is`(true))
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.springframework.boot:spring-boot-starter:3.0.0'
              implementation 'ch.qos.logback:logback-classic:1.0.0'
              implementation 'org.apache.logging.log4j:log4j-api:2.0'
              implementation 'org.springframework:spring-jdbc:6.0.0'
              // keeps versions
              implementation 'io.sentry:sentry-jdbc:6.10.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-jakarta:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-boot-jakarta:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-logback:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-log4j2:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-jdbc:6.10.0" in result.output }

        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `logback is added`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'ch.qos.logback:logback-classic:1.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-logback:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `log4j2 is added`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.apache.logging.log4j:log4j-api:2.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-log4j2:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for spring-jdbc`() {
        assumeThat(getJavaVersion() >= 17, `is`(true))
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.springframework:spring-jdbc:6.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for hsql`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.hsqldb:hsqldb:2.7.2'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        println(result.output)

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for mysql`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'mysql:mysql-connector-java:8.0.33'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for mariadb`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.mariadb.jdbc:mariadb-java-client:3.1.4'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for postgres`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.postgresql:postgresql:42.6.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for oracle ojdbc5`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'com.oracle.database.jdbc:ojdbc5:11.2.0.4'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `jdbc is added for oracle ojdbc11`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'com.oracle.database.jdbc:ojdbc11:23.2.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-jdbc:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `kotlin-extensions is added`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-kotlin-extensions:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring is added for Spring 5`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.springframework:spring-core:5.1.2.RELEASE'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-spring:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-jakarta:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-jakarta:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring-jakarta is added for Spring 6`() {
        assumeThat(getJavaVersion() >= 17, `is`(true))
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.springframework:spring-core:6.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-spring:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-jakarta:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-jakarta:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring-boot-starter is added for Spring Boot 2`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.springframework.boot:spring-boot-starter:2.1.0.RELEASE'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-spring:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-boot:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-jakarta:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot-jakarta:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `spring-boot-starter-jakarta is added for Spring Boot 3`() {
        assumeThat(getJavaVersion() >= 17, `is`(true))
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.springframework.boot:spring-boot-starter:3.0.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.28.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertFalse { "io.sentry:sentry-spring:6.28.0" in result.output }
        assertFalse { "io.sentry:sentry-spring-boot:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-jakarta:6.28.0" in result.output }
        assertTrue { "io.sentry:sentry-spring-boot-jakarta:6.28.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    @Test
    fun `quartz is added`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
                id "java"
                id "io.sentry.jvm.gradle"
            }
            dependencies {
              implementation 'org.quartz-scheduler:quartz:2.3.0'
            }

            sentry.autoInstallation.enabled = true
            sentry.autoInstallation.sentryVersion = "6.30.0"
            """.trimIndent()
        )

        val result = runListDependenciesTask()

        assertTrue { "io.sentry:sentry-quartz:6.30.0" in result.output }
        // ensure all dependencies could be resolved
        assertFalse { "FAILED" in result.output }
    }

    private fun runListDependenciesTask() = runner
        .appendArguments("app:dependencies")
        .build()

    private fun getJavaVersion(): Int {
        var version = System.getProperty("java.version")
        if (version.startsWith("1.")) {
            version = version.substring(2, 3)
        } else {
            val dot = version.indexOf(".")
            if (dot != -1) {
                version = version.substring(0, dot)
            }
        }
        return version.toInt()
    }
}
