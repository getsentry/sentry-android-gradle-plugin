package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.SentryMappingFileProvider.getMappingFile
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SentryMappingFileProviderTest {

    private val sep = File.separator

    @Test
    fun `getMappingFile works correctly when minify enabled`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
            buildTypes {
                it.all { buildType ->
                    buildType.setMinifyEnabled(true)
                }
            }
        }

        // This forces the project to be evaluated
        project.getTasksByName("assembleDebug", false)

        val debugVariant = android.applicationVariants.first { it.name == "debug" }
        assertTrue { getMappingFile(project, debugVariant)!!.endsWith("build${sep}outputs${sep}mapping${sep}debug${sep}mapping.txt") }

        val releaseVariant = android.applicationVariants.first { it.name == "release" }
        assertTrue { getMappingFile(project, releaseVariant)!!.endsWith("build${sep}outputs${sep}mapping${sep}release${sep}mapping.txt") }
    }

    @Test
    fun `getMappingFile returns null when minify disabled`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(AppExtension::class.java).apply {
            compileSdkVersion(30)
        }

        // This forces the project to be evaluated
        project.getTasksByName("assembleDebug", false)

        val debugVariant = android.applicationVariants.first { it.name == "debug" }
        assertNull(getMappingFile(project, debugVariant))

        val releaseVariant = android.applicationVariants.first { it.name == "release" }
        assertNull(getMappingFile(project, releaseVariant))
    }
}
