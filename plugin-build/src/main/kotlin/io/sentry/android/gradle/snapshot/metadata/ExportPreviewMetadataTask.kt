package io.sentry.android.gradle.snapshot.metadata

import groovy.json.JsonOutput
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class ExportPreviewMetadataTask : DefaultTask() {

  init {
    description = "Exports Compose @Preview metadata to a JSON file using ASM bytecode scanning"
  }

  @get:Input abstract val includePrivatePreviews: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputClasspath: ConfigurableFileCollection

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun export() {
    val scanner = PreviewMethodScanner(includePrivatePreviews.get())

    // First pass: discover custom preview annotations.
    // Two iterations handle nested custom annotations where A references B but B was
    // processed after A in the first iteration.
    val customAnnotations = mutableMapOf<String, CustomPreviewAnnotation>()
    repeat(2) {
      forEachClassEntry { _, bytes -> scanner.findCustomAnnotations(bytes, customAnnotations) }
    }

    // Second pass: find preview methods
    val previews = mutableListOf<PreviewMetadata>()
    forEachClassEntry { relativePath, bytes ->
      scanClassFile(bytes, relativePath, scanner, customAnnotations, previews)
    }

    val export = PreviewMetadataExport(previews = previews)
    val json = JsonOutput.prettyPrint(JsonOutput.toJson(export.toMap()))

    val outFile = outputFile.get().asFile
    outFile.parentFile.mkdirs()
    outFile.writeText(json)

    logger.lifecycle("Exported ${previews.size} preview(s) to ${outFile.absolutePath}")
  }

  private fun forEachClassEntry(action: (relativePath: String, bytes: ByteArray) -> Unit) {
    inputClasspath.files.forEach { file ->
      if (file.isDirectory) {
        file
          .walkTopDown()
          .filter { it.isFile && it.extension == "class" }
          .forEach { classFile ->
            val relativePath = classFile.relativeTo(file).path
            action(relativePath, classFile.readBytes())
          }
      } else if (file.isFile && (file.name.endsWith(".jar") || file.name.endsWith(".zip"))) {
        ZipInputStream(file.inputStream().buffered()).use { zis ->
          generateSequence { zis.nextEntry }
            .filter { !it.isDirectory && it.name.endsWith(".class") }
            .forEach { entry ->
              action(entry.name, zis.readBytes())
              zis.closeEntry()
            }
        }
      }
    }
  }

  private fun scanClassFile(
    bytes: ByteArray,
    relativePath: String,
    scanner: PreviewMethodScanner,
    customAnnotations: Map<String, CustomPreviewAnnotation>,
    results: MutableList<PreviewMetadata>,
  ) {
    val scanResult = scanner.fullScan(bytes, customAnnotations)
    val methods = scanResult.previewMethods
    if (methods.isEmpty()) return
    val sourceFileName = scanResult.sourceFile

    val rawClassName = relativePath.removeSuffix(".class").replace('/', '.').replace('\\', '.')
    // Strip Kt suffix — Kotlin top-level functions compile to FooKt.class
    val className = rawClassName.removeSuffix("Kt")

    for (method in methods) {
      val config = method.config
      val device =
        if (config.device != null || config.widthDp != null || config.heightDp != null) {
          DeviceMetadata(
            deviceSpec = config.device,
            widthDp = config.widthDp,
            heightDp = config.heightDp,
          )
        } else {
          null
        }

      val previewParam =
        method.previewParameter?.let {
          PreviewParameterMetadata(
            parameterName = it.parameterName,
            providerClassFqn = it.providerClassFqn,
            limit = it.limit,
            index = it.index,
          )
        }

      results.add(
        PreviewMetadata(
          className = className,
          methodName = method.methodName,
          sourceFileName = sourceFileName,
          previewName = config.name,
          configuration =
            PreviewConfiguration(
              apiLevel = config.apiLevel,
              locale = config.locale,
              fontScale = config.fontScale,
              uiMode = config.uiMode,
              showSystemUi = config.showSystemUi,
              showBackground = config.showBackground,
              backgroundColor = config.backgroundColor,
              group = config.group,
              wallpaper = config.wallpaper,
            ),
          device = device,
          previewParameter = previewParam,
        )
      )
    }
  }

  companion object {
    private const val ARTIFACT_TYPE = "artifactType"

    fun register(
      project: Project,
      extension: SentrySnapshotMetadataExtension,
      variantName: String,
    ): TaskProvider<ExportPreviewMetadataTask> {
      val taskSuffix = variantName.capitalized
      val compileTaskName = "compile${taskSuffix}Kotlin"

      return project.tasks.register(
        "exportPreviewMetadata$taskSuffix",
        ExportPreviewMetadataTask::class.java,
      ) { task ->
        task.includePrivatePreviews.set(extension.includePrivatePreviews)

        // Local compiled classes
        task.inputClasspath.from(
          project.tasks.named(compileTaskName).map { it.outputs.files }
        )

        // Dependency project classes
        val variantClasspath =
          project.configurations.getByName("${variantName}RuntimeClasspath")
        task.inputClasspath.from(
          variantClasspath.incoming
            .artifactView { view ->
              view.componentFilter { id -> id is ProjectComponentIdentifier }
              view.attributes { attrs ->
                attrs.attribute(Attribute.of(ARTIFACT_TYPE, String::class.java), "android-classes")
              }
            }
            .files
        )

        task.outputFile.set(
          project.layout.buildDirectory.file(
            "sentry-snapshots/preview-metadata/$variantName/preview-metadata.json"
          )
        )

        task.dependsOn(compileTaskName)
      }
    }
  }
}
