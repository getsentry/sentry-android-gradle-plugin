package io.sentry.android.gradle.snapshot.preview

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes

/** Two-pass ASM scanner that discovers @Preview composables from compiled .class files. */
internal object PreviewScanner {

  fun scan(classesDir: File, includePrivatePreviews: Boolean): List<PreviewSnapshotConfig> {
    if (!classesDir.exists()) return emptyList()

    // Pass 1: find annotation classes that are themselves meta-annotated with @Preview
    val customAnnotations = findCustomPreviewAnnotations(classesDir)

    // Pass 2: find methods annotated with @Preview (direct, container, or custom)
    val results = mutableListOf<PreviewSnapshotConfig>()
    classesDir
      .walk()
      .filter { it.name.endsWith(".class") }
      .forEach { classFile ->
        results.addAll(
          extractPreviewMethods(classFile.readBytes(), includePrivatePreviews, customAnnotations)
        )
      }
    return results
  }

  private fun findCustomPreviewAnnotations(classesDir: File): Map<String, CustomPreviewAnnotation> {
    val annotations = ConcurrentHashMap<String, CustomPreviewAnnotation>()

    fun scanOnce() {
      classesDir
        .walk()
        .filter { it.name.endsWith(".class") }
        .forEach { classFile ->
          val reader = ClassReader(classFile.readBytes())
          val visitor = FindCustomPreviewClassVisitor(annotations)
          reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        }
    }

    // Run twice to resolve annotation dependency ordering
    scanOnce()
    scanOnce()

    // Only keep annotations that actually have preview configs
    return annotations.filter { it.value.previewConfigs.isNotEmpty() }
  }

  private fun extractPreviewMethods(
    classBytes: ByteArray,
    includePrivatePreviews: Boolean,
    customAnnotations: Map<String, CustomPreviewAnnotation>,
  ): List<PreviewSnapshotConfig> {
    val reader = ClassReader(classBytes)
    val results = mutableListOf<PreviewSnapshotConfig>()
    val visitor =
      SnapshotClassVisitor(
        Opcodes.ASM9,
        reader.className,
        results,
        includePrivatePreviews,
        customAnnotations,
      )
    reader.accept(visitor, ClassReader.EXPAND_FRAMES)
    return results
  }
}

/** Returns built-in preview configs for well-known multi-preview annotation descriptors. */
@Suppress("detekt.LongMethod")
internal fun previewConfigForAnnotation(descriptor: String?): List<PreviewConfig>? {
  return when (descriptor) {
    PREVIEW_LIGHT_DARK_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(name = "light"),
        PreviewConfig(name = "dark", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL),
      )
    PREVIEW_SCREEN_SIZES_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(name = "Phone", device = PHONE, showSystemUi = true),
        PreviewConfig(
          name = "Phone - Landscape",
          device = "spec:width = 411dp, height = 891dp, orientation = landscape, dpi = 420",
          showSystemUi = true,
        ),
        PreviewConfig(name = "Unfolded Foldable", device = FOLDABLE, showSystemUi = true),
        PreviewConfig(name = "Tablet", device = TABLET, showSystemUi = true),
        PreviewConfig(name = "Desktop", device = DESKTOP, showSystemUi = true),
      )
    PREVIEW_FONT_SCALE_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(name = "85%", fontScale = 0.85f),
        PreviewConfig(name = "100%", fontScale = 1.0f),
        PreviewConfig(name = "115%", fontScale = 1.15f),
        PreviewConfig(name = "130%", fontScale = 1.3f),
        PreviewConfig(name = "150%", fontScale = 1.5f),
        PreviewConfig(name = "180%", fontScale = 1.8f),
        PreviewConfig(name = "200%", fontScale = 2f),
      )
    PREVIEW_DYNAMIC_COLORS_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(name = "Red", wallpaper = 0),
        PreviewConfig(name = "Blue", wallpaper = 1),
        PreviewConfig(name = "Green", wallpaper = 2),
        PreviewConfig(name = "Yellow", wallpaper = 3),
      )
    PREVIEW_WEAR_SMALL_ROUND_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(
          device = SMALL_ROUND,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Small Round",
          showSystemUi = true,
        )
      )
    PREVIEW_WEAR_LARGE_ROUND_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(
          device = LARGE_ROUND,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Large Round",
          showSystemUi = true,
        )
      )
    PREVIEW_WEAR_SQUARE_ANNOTATION_DESC ->
      listOf(
        PreviewConfig(
          device = SQUARE,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Square",
          showSystemUi = true,
        )
      )
    PREVIEW_WEAR_FONT_SCALES -> {
      val base =
        PreviewConfig(
          device = SMALL_ROUND,
          showSystemUi = true,
          backgroundColor = 0xff000000,
          showBackground = true,
        )
      listOf(
        base.copy(group = "Fonts - Small", fontScale = 0.94f),
        base.copy(group = "Fonts - Normal", fontScale = 1f),
        base.copy(group = "Fonts - Medium", fontScale = 1.06f),
        base.copy(group = "Fonts - Large", fontScale = 1.12f),
        base.copy(group = "Fonts - Larger", fontScale = 1.18f),
        base.copy(group = "Fonts - Largest", fontScale = 1.24f),
      )
    }
    PREVIEW_WEAR_DEVICES_ANNOTATION_DESC -> {
      val base =
        PreviewConfig(showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
      listOf(
        base.copy(device = SMALL_ROUND, group = "Devices - Small Round"),
        base.copy(device = LARGE_ROUND, group = "Devices - Large Round"),
        base.copy(device = SQUARE, group = "Devices - Small Square"),
      )
    }
    else -> null
  }
}
