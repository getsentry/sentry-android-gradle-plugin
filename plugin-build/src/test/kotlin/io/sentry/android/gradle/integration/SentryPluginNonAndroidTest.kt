package io.sentry.android.gradle.integration

import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginNonAndroidTest :
    BaseSentryNonAndroidPluginTest(GradleVersion.current().version) {

    @Test
    fun `telemetry can be disabled`() {
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
              implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2'
              implementation 'org.postgresql:postgresql:42.6.0'
              implementation 'com.graphql-java:graphql-java:17.3'
            }

            sentry {
                telemetry = false
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:collectDependencies")
            .build()

        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
        assertTrue(result.output) { "Sentry telemetry has been disabled." in result.output }
    }

    @Test
    fun `telemetry is enabled by default`() {
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
              implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2'
              implementation 'org.postgresql:postgresql:42.6.0'
              implementation 'com.graphql-java:graphql-java:17.3'
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:collectDependencies")
            .build()

        assertTrue(result.output) { "BUILD SUCCESSFUL" in result.output }
        assertTrue(result.output) { "Sentry telemetry is enabled." in result.output }
    }
}
