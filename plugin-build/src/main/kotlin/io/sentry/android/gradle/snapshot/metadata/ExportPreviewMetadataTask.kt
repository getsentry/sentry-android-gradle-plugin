package io.sentry.android.gradle.snapshot.metadata

import com.android.build.gradle.BaseExtension
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
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

  @get:Input abstract val scanPackages: ListProperty<String>

  @get:Input abstract val namespace: Property<String>

  @get:Input abstract val includePrivatePreviews: Property<Boolean>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val mergedClassesDir: DirectoryProperty

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun export() {
    val packages = scanPackages.get().ifEmpty { listOf(namespace.get()) }
    val packagePaths = packages.map { it.replace('.', '/') }
    val scanner = PreviewMethodScanner(includePrivatePreviews.get())
    val rootDir = mergedClassesDir.get().asFile

    // First pass: discover custom preview annotations.
    // Two iterations handle nested custom annotations where A references B but B was
    // processed after A in the first iteration.
    val customAnnotations = mutableMapOf<String, CustomPreviewAnnotation>()
    repeat(2) {
      rootDir
        .walk()
        .filter { it.isFile && it.name.endsWith(".class") }
        .forEach { file -> scanner.findCustomAnnotations(file.readBytes(), customAnnotations) }
    }

    // Second pass: find preview methods (filtered to scanned packages)
    val previews = mutableListOf<PreviewMetadata>()
    rootDir
      .walk()
      .filter { it.isFile && it.name.endsWith(".class") }
      .forEach { file ->
        val relativePath = file.relativeTo(rootDir).path
        if (matchesPackage(relativePath, packagePaths)) {
          scanClassFile(file.readBytes(), relativePath, scanner, customAnnotations, previews)
        }
      }

    val export = PreviewMetadataExport(scannedPackages = packages, previews = previews)
    val json = JsonOutput.prettyPrint(JsonOutput.toJson(export.toMap()))

    val outFile = outputFile.get().asFile
    outFile.parentFile.mkdirs()
    outFile.writeText(json)

    logger.lifecycle("Exported ${previews.size} preview(s) to ${outFile.absolutePath}")
  }

  private fun matchesPackage(classPath: String, packagePaths: List<String>): Boolean {
    return packagePaths.any { classPath.startsWith("$it/") }
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

    fun register(
      project: Project,
      extension: SentrySnapshotMetadataExtension,
      android: BaseExtension,
      mergeTask: TaskProvider<MergeClassesTask>,
    ): TaskProvider<ExportPreviewMetadataTask> {
      return project.tasks.register(
        "exportPreviewMetadata",
        ExportPreviewMetadataTask::class.java,
      ) { task ->
        task.scanPackages.set(extension.packageTrees)
        task.namespace.set(project.provider { android.namespace ?: "" })
        task.includePrivatePreviews.set(extension.includePrivatePreviews)
        task.mergedClassesDir.set(mergeTask.flatMap { it.outputDir })

        task.outputFile.set(
          project.layout.buildDirectory.file(
            "sentry-snapshots/preview-metadata/preview-metadata.json"
          )
        )
      }
    }
  }
}
