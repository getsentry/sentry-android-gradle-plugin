package io.sentry.android.gradle.snapshot.metadata

import com.android.build.gradle.BaseExtension
import groovy.json.JsonOutput
import java.io.File
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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

  @get:Input abstract val scanPackages: ListProperty<String>

  @get:Input abstract val namespace: Property<String>

  @get:Input abstract val includePrivatePreviews: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val compiledClassesDirs: ConfigurableFileCollection

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun export() {
    val packages = scanPackages.get().ifEmpty { listOf(namespace.get()) }
    val packagePaths = packages.map { it.replace('.', '/') }
    val scanner = PreviewMethodScanner(includePrivatePreviews.get())
    val previews = mutableListOf<PreviewMetadata>()

    compiledClassesDirs.files.forEach { root ->
      if (root.isDirectory) {
        scanDirectory(root, root, packagePaths, scanner, previews)
      } else if (root.isFile && (root.name.endsWith(".jar") || root.name.endsWith(".zip"))) {
        scanJar(root, packagePaths, scanner, previews)
      }
    }

    val export = PreviewMetadataExport(scannedPackages = packages, previews = previews)
    val json = JsonOutput.prettyPrint(JsonOutput.toJson(export.toMap()))

    val outFile = outputFile.get().asFile
    outFile.parentFile.mkdirs()
    outFile.writeText(json)

    logger.lifecycle("Exported ${previews.size} preview(s) to ${outFile.absolutePath}")
  }

  private fun scanDirectory(
    root: File,
    dir: File,
    packagePaths: List<String>,
    scanner: PreviewMethodScanner,
    results: MutableList<PreviewMetadata>,
  ) {
    dir.listFiles()?.forEach { file ->
      if (file.isDirectory) {
        scanDirectory(root, file, packagePaths, scanner, results)
      } else if (file.name.endsWith(".class")) {
        val relativePath = file.relativeTo(root).path
        if (matchesPackage(relativePath, packagePaths)) {
          scanClassFile(file.readBytes(), relativePath, scanner, results)
        }
      }
    }
  }

  private fun scanJar(
    jarFile: File,
    packagePaths: List<String>,
    scanner: PreviewMethodScanner,
    results: MutableList<PreviewMetadata>,
  ) {
    ZipInputStream(jarFile.inputStream().buffered()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".class")) {
          if (matchesPackage(entry.name, packagePaths)) {
            scanClassFile(zip.readBytes(), entry.name, scanner, results)
          }
        }
        entry = zip.nextEntry
      }
    }
  }

  private fun matchesPackage(classPath: String, packagePaths: List<String>): Boolean {
    return packagePaths.any { classPath.startsWith("$it/") }
  }

  private fun scanClassFile(
    bytes: ByteArray,
    relativePath: String,
    scanner: PreviewMethodScanner,
    results: MutableList<PreviewMetadata>,
  ) {
    val scanResult = scanner.fullScan(bytes)
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
            ),
          device = device,
        )
      )
    }
  }

  companion object {
    private const val ARTIFACT_TYPE = "artifactType"

    fun register(
      project: Project,
      extension: SentrySnapshotMetadataExtension,
      android: BaseExtension,
    ): TaskProvider<ExportPreviewMetadataTask> {
      return project.tasks.register(
        "exportPreviewMetadata",
        ExportPreviewMetadataTask::class.java,
      ) { task ->
        task.scanPackages.set(extension.packageTrees)
        task.namespace.set(project.provider { android.namespace ?: "" })
        task.includePrivatePreviews.set(extension.includePrivatePreviews)

        // Local compiled classes from compileDebugKotlin
        task.compiledClassesDirs.from(
          project.tasks.named("compileDebugKotlin").map { it.outputs.files }
        )

        // Project dependency classes from debugRuntimeClasspath
        val debugClasspath = project.configurations.getByName("debugRuntimeClasspath")
        task.compiledClassesDirs.from(
          debugClasspath.incoming
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
            "sentry-snapshots/preview-metadata/preview-metadata.json"
          )
        )

        task.dependsOn("compileDebugKotlin")
      }
    }
  }
}
