package io.sentry.android.gradle.snapshot.metadata

import com.android.build.gradle.BaseExtension
import java.io.File
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class MergeClassesTask : DefaultTask() {

  init {
    description =
      "Merges compiled classes from local and dependency sources into a single directory"
  }

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputClasspath: ConfigurableFileCollection

  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun merge() {
    val outputDirectory = outputDir.get().asFile
    outputDirectory.deleteRecursively()
    outputDirectory.mkdirs()

    val seenEntries = mutableSetOf<String>()

    inputClasspath.files.forEach { file ->
      if (file.isDirectory) {
        mergeDirectory(file, outputDirectory, seenEntries)
      } else if (file.isFile && (file.name.endsWith(".jar") || file.name.endsWith(".zip"))) {
        mergeJar(file, outputDirectory, seenEntries)
      }
    }
  }

  private fun mergeDirectory(
    inputDir: File,
    outputDirectory: File,
    seenEntries: MutableSet<String>,
  ) {
    inputDir
      .walkTopDown()
      .filter { it.isFile && it.extension == "class" }
      .forEach { classFile ->
        val relativePath = classFile.relativeTo(inputDir).path
        if (seenEntries.add(relativePath)) {
          val target = File(outputDirectory, relativePath)
          target.parentFile.mkdirs()
          classFile.copyTo(target)
        }
      }
  }

  private fun mergeJar(jar: File, outputDirectory: File, seenEntries: MutableSet<String>) {
    ZipInputStream(jar.inputStream().buffered()).use { zis ->
      generateSequence { zis.nextEntry }
        .filter { !it.isDirectory && it.name.endsWith(".class") }
        .filter { seenEntries.add(it.name) }
        .forEach { entry ->
          val target = File(outputDirectory, entry.name)
          target.parentFile.mkdirs()
          target.outputStream().use { output -> zis.copyTo(output) }
          zis.closeEntry()
        }
    }
  }

  companion object {
    private const val ARTIFACT_TYPE = "artifactType"

    fun register(project: Project, android: BaseExtension): TaskProvider<MergeClassesTask> {
      return project.tasks.register("mergeSnapshotClasses", MergeClassesTask::class.java) { task ->
        // TODO Debug is hard coded here. we should allow different variants
        task.inputClasspath.from(project.tasks.named("compileDebugKotlin").map { it.outputs.files })

        val debugClasspath = project.configurations.getByName("debugRuntimeClasspath")
        task.inputClasspath.from(
          debugClasspath.incoming
            .artifactView { view ->
              view.componentFilter { id -> id is ProjectComponentIdentifier }
              view.attributes { attrs ->
                attrs.attribute(Attribute.of(ARTIFACT_TYPE, String::class.java), "android-classes")
              }
            }
            .files
        )

        task.outputDir.set(project.layout.buildDirectory.dir("sentry-snapshots/merged-classes"))

        task.dependsOn("compileDebugKotlin")
      }
    }
  }
}
