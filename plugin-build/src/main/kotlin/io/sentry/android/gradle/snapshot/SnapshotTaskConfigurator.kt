package io.sentry.android.gradle.snapshot

import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.extensions.SnapshotExtension
import io.sentry.android.gradle.snapshot.codegen.SnapshotTestTemplate
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

/** Wires the scan task, generated test template, and test task system properties together. */
internal object SnapshotTaskConfigurator {

  fun configure(project: Project, extension: SnapshotExtension) {
    val variant = extension.variant.get()
    val variantCapitalized = variant.replaceFirstChar { it.titlecase() }

    // Write the template test file (always same content, written once at configuration time)
    val generatedTestDir =
      project.layout.buildDirectory.dir("generated/sentry/snapshot/test").get().asFile
    writeTemplateTestFile(generatedTestDir)

    // Add generated dir to the test source set
    val android = project.extensions.getByType(AppExtension::class.java)
    android.sourceSets.getByName("test").java.srcDir(generatedTestDir)

    // Register the ASM scan task
    val classesDirs = project.layout.buildDirectory.dir("tmp/kotlin-classes/$variant")
    val scanTask =
      ScanPreviewsTask.register(project, variant, classesDirs, extension.includePrivatePreviews)
    scanTask.configure { it.dependsOn("compile${variantCapitalized}Kotlin") }

    // Wire scan task into test execution and pass config file path as system property
    project.tasks
      .matching { it.name == "test${variantCapitalized}UnitTest" || it.name == "testDebugUnitTest" }
      .configureEach { testTask ->
        testTask.dependsOn(scanTask)
        if (testTask is Test) {
          testTask.jvmArgumentProviders.add(
            ConfigFileArgumentProvider(
              scanTask.flatMap { it.outputFile }.map { it.asFile.absolutePath }
            )
          )
        }
      }
  }

  private fun writeTemplateTestFile(generatedTestDir: File) {
    val packageDir =
      File(generatedTestDir, SnapshotTestTemplate.PACKAGE_NAME.replace('.', File.separatorChar))
    packageDir.mkdirs()
    val testFile = File(packageDir, "${SnapshotTestTemplate.CLASS_NAME}.kt")
    testFile.writeText(SnapshotTestTemplate.content)
  }

  /** Lazily provides the -D system property pointing to the JSON config file. */
  private class ConfigFileArgumentProvider(
    private val configFilePath: org.gradle.api.provider.Provider<String>
  ) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
      listOf("-Dsentry.snapshot.configFile=${configFilePath.get()}")
  }
}
