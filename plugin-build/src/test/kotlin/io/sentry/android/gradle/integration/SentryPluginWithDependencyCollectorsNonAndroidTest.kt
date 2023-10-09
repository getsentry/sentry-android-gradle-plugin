package io.sentry.android.gradle.integration

import kotlin.test.assertTrue
import org.gradle.util.GradleVersion
import org.junit.Test

class SentryPluginWithDependencyCollectorsNonAndroidTest :
    BaseSentryNonAndroidPluginTest(GradleVersion.current().version) {

    @Test
    fun `does not break when there are plugins that collect dependencies applied`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "java"
              id "io.sentry.jvm.gradle"
              id "com.mikepenz.aboutlibraries.plugin"
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
              autoUploadProguardMapping = false
              autoInstallation.enabled = true
            }
            """.trimIndent()
        )

        val result = runner
            .appendArguments("app:collectDependencies")
            .build()

        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    override val additionalBuildClasspath: String =
        """
        classpath 'com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:10.6.1'
        """.trimIndent()
}
