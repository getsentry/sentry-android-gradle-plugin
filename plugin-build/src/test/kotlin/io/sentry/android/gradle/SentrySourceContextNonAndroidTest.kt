package io.sentry.android.gradle

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentrySourceContextNonAndroidTest(
    gradleVersion: String
) : BaseSentryNonAndroidPluginTest(gradleVersion) {

    @Test
    fun `skips bundle and upload tasks if no sources`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "org.jetbrains.kotlin.jvm"
              id "io.sentry.jvm.gradle"
            }

            sentry {
              includeSourceContext = true
            }
            """.trimIndent()
        )
        val result = runner
            .appendArguments("app:assemble")
            .build()

        assertEquals(
            result.task(":app:sentryUploadSourceBundleJava")?.outcome,
            SKIPPED
        )
        assertEquals(
            result.task(":app:sentryBundleSourcesJava")?.outcome,
            SKIPPED
        )
        assertTrue { "BUILD SUCCESSFUL" in result.output }
    }

    @Test
    fun `bundles source context`() {
        appBuildFile.writeText(
            // language=Groovy
            """
            plugins {
              id "org.jetbrains.kotlin.jvm"
              id "io.sentry.jvm.gradle"
            }

            sentry {
              includeSourceContext = true
              autoUploadSourceContext = false
              autoUploadProguardMapping = false
              additionalSourceDirsForSourceContext = ["src/custom/kotlin"]
            }
            """.trimIndent()
        )

        val ktContents = testProjectDir.withDummyKtFile()
        val javaContents = testProjectDir.withDummyJavaFile()
        val customContents = testProjectDir.withDummyCustomFile()

        val result = runner
            .appendArguments("app:assemble")
            .build()

        assertTrue { "BUILD SUCCESSFUL" in result.output }

        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/Example.jvm",
            ktContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/com/example/TestJava.jvm",
            javaContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
        verifySourceBundleContents(
            testProjectDir.root,
            "files/_/_/io/other/TestCustom.jvm",
            customContents,
            variant = "java",
            archivePath = "app/build/libs/app.jar"
        )
    }
}
