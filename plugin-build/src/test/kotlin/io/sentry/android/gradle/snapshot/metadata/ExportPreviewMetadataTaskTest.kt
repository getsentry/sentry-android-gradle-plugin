package io.sentry.android.gradle.snapshot.metadata

import groovy.json.JsonSlurper
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class ExportPreviewMetadataTaskTest {

  @get:Rule val tmpDir = TemporaryFolder()

  @Test
  fun `exports preview metadata from class files to JSON`() {
    val classesDir = tmpDir.newFolder("classes", "com", "example")
    writePreviewClass(
      dir = classesDir,
      fileName = "GreetingKt.class",
      internalName = "com/example/GreetingKt",
      sourceFile = "Greeting.kt",
      methods =
        listOf(
          MethodSpec("GreetingPreview"),
          MethodSpec("DarkGreetingPreview", mapOf("uiMode" to 0x20, "showBackground" to true)),
        ),
    )

    val outputFile = tmpDir.newFile("preview-metadata.json")
    val task = createTask(classesDir.parentFile.parentFile, outputFile, listOf("com.example"))

    task.export()

    assertTrue(outputFile.exists())
    val parsed = parseJson(outputFile)
    @Suppress("UNCHECKED_CAST") val previews = parsed["previews"] as List<Map<String, Any?>>
    assertEquals(2, previews.size)

    val first = previews[0]
    assertEquals("com.example.Greeting", first["className"])
    assertEquals("GreetingPreview", first["methodName"])
    assertEquals("Greeting.kt", first["sourceFileName"])

    val second = previews[1]
    assertEquals("DarkGreetingPreview", second["methodName"])
    @Suppress("UNCHECKED_CAST") val config = second["configuration"] as Map<String, Any?>
    assertEquals(0x20, config["uiMode"])
    assertEquals(true, config["showBackground"])
  }

  @Test
  fun `falls back to namespace when scanPackages is empty`() {
    val classesDir = tmpDir.newFolder("classes", "io", "app")
    writePreviewClass(
      dir = classesDir,
      fileName = "ScreenKt.class",
      internalName = "io/app/ScreenKt",
      sourceFile = "Screen.kt",
      methods = listOf(MethodSpec("ScreenPreview")),
    )

    val outputFile = tmpDir.newFile("output.json")
    val task =
      createTask(
        classesDir = classesDir.parentFile.parentFile,
        outputFile = outputFile,
        scanPackages = emptyList(),
        namespace = "io.app",
      )

    task.export()

    val parsed = parseJson(outputFile)
    @Suppress("UNCHECKED_CAST") val scannedPackages = parsed["scannedPackages"] as List<String>
    assertEquals(listOf("io.app"), scannedPackages)

    @Suppress("UNCHECKED_CAST") val previews = parsed["previews"] as List<Map<String, Any?>>
    assertEquals(1, previews.size)
  }

  @Test
  fun `skips classes outside scanned packages`() {
    val inPackage = tmpDir.newFolder("classes", "com", "example")
    val outsidePackage = tmpDir.newFolder("classes", "com", "other")

    writePreviewClass(
      dir = inPackage,
      fileName = "InsideKt.class",
      internalName = "com/example/InsideKt",
      methods = listOf(MethodSpec("InsidePreview")),
    )
    writePreviewClass(
      dir = outsidePackage,
      fileName = "OutsideKt.class",
      internalName = "com/other/OutsideKt",
      methods = listOf(MethodSpec("OutsidePreview")),
    )

    val outputFile = tmpDir.newFile("output.json")
    val task = createTask(inPackage.parentFile.parentFile, outputFile, listOf("com.example"))

    task.export()

    val parsed = parseJson(outputFile)
    @Suppress("UNCHECKED_CAST") val previews = parsed["previews"] as List<Map<String, Any?>>
    assertEquals(1, previews.size)
    assertEquals("InsidePreview", previews[0]["methodName"])
  }

  @Test
  fun `exports device metadata when device fields are present`() {
    val classesDir = tmpDir.newFolder("classes", "com", "example")
    writePreviewClass(
      dir = classesDir,
      fileName = "DeviceKt.class",
      internalName = "com/example/DeviceKt",
      methods =
        listOf(
          MethodSpec(
            "TabletPreview",
            mapOf(
              "device" to "spec:width=800dp,height=1280dp",
              "widthDp" to 800,
              "heightDp" to 1280,
            ),
          )
        ),
    )

    val outputFile = tmpDir.newFile("output.json")
    val task = createTask(classesDir.parentFile.parentFile, outputFile, listOf("com.example"))

    task.export()

    val parsed = parseJson(outputFile)
    @Suppress("UNCHECKED_CAST") val previews = parsed["previews"] as List<Map<String, Any?>>
    assertEquals(1, previews.size)

    @Suppress("UNCHECKED_CAST") val device = previews[0]["device"] as Map<String, Any?>
    assertNotNull(device)
    assertEquals("spec:width=800dp,height=1280dp", device["deviceSpec"])
    assertEquals(800, device["widthDp"])
    assertEquals(1280, device["heightDp"])
  }

  @Test
  fun `produces empty previews list for class with no previews`() {
    val classesDir = tmpDir.newFolder("classes", "com", "example")
    writePreviewClass(
      dir = classesDir,
      fileName = "PlainKt.class",
      internalName = "com/example/PlainKt",
      methods = emptyList(),
    )

    val outputFile = tmpDir.newFile("output.json")
    val task = createTask(classesDir.parentFile.parentFile, outputFile, listOf("com.example"))

    task.export()

    val parsed = parseJson(outputFile)
    @Suppress("UNCHECKED_CAST") val previews = parsed["previews"] as List<Map<String, Any?>>
    assertTrue(previews.isEmpty())
  }

  private fun createTask(
    classesDir: File,
    outputFile: File,
    scanPackages: List<String>,
    namespace: String = "com.example",
  ): ExportPreviewMetadataTask {
    val project = ProjectBuilder.builder().build()
    return project.tasks
      .register("testExportPreviewMetadata", ExportPreviewMetadataTask::class.java) { task ->
        task.scanPackages.set(scanPackages)
        task.namespace.set(namespace)
        task.includePrivatePreviews.set(false)
        task.compiledClassesDirs.from(classesDir)
        task.outputFile.set(outputFile)
      }
      .get()
  }

  private data class MethodSpec(
    val name: String,
    val annotationFields: Map<String, Any> = emptyMap(),
  )

  private fun writePreviewClass(
    dir: File,
    fileName: String,
    internalName: String,
    sourceFile: String? = null,
    methods: List<MethodSpec>,
  ) {
    val cw = ClassWriter(0)
    cw.visit(
      Opcodes.V11,
      Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
      internalName,
      null,
      "java/lang/Object",
      null,
    )
    if (sourceFile != null) {
      cw.visitSource(sourceFile, null)
    }

    for (method in methods) {
      val mv =
        cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, method.name, "()V", null, null)
      val av = mv.visitAnnotation("Landroidx/compose/ui/tooling/preview/Preview;", true)
      for ((key, value) in method.annotationFields) {
        av.visit(key, value)
      }
      av.visitEnd()
      mv.visitEnd()
    }

    cw.visitEnd()
    File(dir, fileName).writeBytes(cw.toByteArray())
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseJson(file: File): Map<String, Any?> {
    return JsonSlurper().parseText(file.readText()) as Map<String, Any?>
  }
}
