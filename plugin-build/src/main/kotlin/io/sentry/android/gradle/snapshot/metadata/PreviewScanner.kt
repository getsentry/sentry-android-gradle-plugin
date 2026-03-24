package io.sentry.android.gradle.snapshot.metadata

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

// region Annotation descriptors

private const val PREVIEW_DESCRIPTOR = "Landroidx/compose/ui/tooling/preview/Preview;"
private const val PREVIEW_CONTAINER_DESCRIPTOR =
  "Landroidx/compose/ui/tooling/preview/Preview\$Container;"
private const val PREVIEW_PARAMETER_DESCRIPTOR =
  "Landroidx/compose/ui/tooling/preview/PreviewParameter;"

private const val PREVIEW_LIGHT_DARK = "Landroidx/compose/ui/tooling/preview/PreviewLightDark;"
private const val PREVIEW_FONT_SCALE = "Landroidx/compose/ui/tooling/preview/PreviewFontScale;"
private const val PREVIEW_SCREEN_SIZES = "Landroidx/compose/ui/tooling/preview/PreviewScreenSizes;"
private const val PREVIEW_DYNAMIC_COLORS =
  "Landroidx/compose/ui/tooling/preview/PreviewDynamicColors;"

private const val WEAR_PREVIEW_SMALL_ROUND =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewSmallRound;"
private const val WEAR_PREVIEW_LARGE_ROUND =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewLargeRound;"
private const val WEAR_PREVIEW_SQUARE =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewSquare;"
private const val WEAR_PREVIEW_FONT_SCALES =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewFontScales;"
private const val WEAR_PREVIEW_DEVICES =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewDevices;"

// endregion

// region Device spec constants
// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-tooling-preview/src/androidMain/kotlin/androidx/compose/ui/tooling/preview/Device.android.kt

private const val PHONE =
  "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420"
private const val FOLDABLE =
  "spec:id=reference_foldable,shape=Normal,width=673,height=841,unit=dp,dpi=420"
private const val TABLET =
  "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240"
private const val DESKTOP =
  "spec:id=reference_desktop,shape=Normal,width=1920,height=1080,unit=dp,dpi=160"
private const val SMALL_ROUND = "id:wearos_small_round"
private const val LARGE_ROUND = "id:wearos_large_round"
private const val SQUARE = "id:wearos_square"

// endregion

// region UI mode constants
// https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/content/res/Configuration.java

private const val UI_MODE_NIGHT_YES: Int = 32
private const val UI_MODE_TYPE_NORMAL: Int = 1

// endregion

// region Data classes

data class PreviewConfig(
  var apiLevel: Int? = null,
  var locale: String? = null,
  var fontScale: Float = 1.0f,
  var uiMode: Int = 0,
  var showSystemUi: Boolean = false,
  var showBackground: Boolean = false,
  var backgroundColor: Long? = null,
  var name: String? = null,
  var group: String? = null,
  var device: String? = null,
  var widthDp: Int? = null,
  var heightDp: Int? = null,
  var wallpaper: Int? = null,
)

data class PreviewParameter(
  val parameterName: String,
  val providerClassFqn: String,
  val limit: Int? = null,
  val index: Int? = null,
)

data class PreviewMethod(
  val methodName: String,
  val config: PreviewConfig,
  val previewParameter: PreviewParameter? = null,
)

data class ScanResult(val previewMethods: List<PreviewMethod>, val sourceFile: String?)

data class CustomPreviewAnnotation(
  val previewConfigs: MutableList<PreviewConfig> = mutableListOf()
)

// endregion

class PreviewMethodScanner(private val includePrivatePreviews: Boolean) {

  fun scan(classBytes: ByteArray): List<PreviewMethod> = fullScan(classBytes).previewMethods

  fun fullScan(classBytes: ByteArray): ScanResult = fullScan(classBytes, emptyMap())

  fun fullScan(
    classBytes: ByteArray,
    customAnnotations: Map<String, CustomPreviewAnnotation>,
  ): ScanResult {
    val reader = ClassReader(classBytes)
    val visitor = PreviewClassVisitor(includePrivatePreviews, customAnnotations)
    reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
    return ScanResult(visitor.previewMethods, visitor.sourceFile)
  }

  fun findCustomAnnotations(
    classBytes: ByteArray,
    accumulator: MutableMap<String, CustomPreviewAnnotation>,
  ) {
    val reader = ClassReader(classBytes)
    val visitor = FindCustomPreviewClassVisitor(accumulator)
    reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
  }
}

// region First pass: discover custom preview annotations

/**
 * Scans class-level annotations to find annotation classes that are themselves annotated with
 *
 * @Preview (or known multipreview annotations, or other custom preview annotations). This enables
 *   support for user-defined multipreview annotations like:
 * ```
 * @Preview(name = "Light")
 * @Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
 * annotation class LightDarkPreviews
 * ```
 */
private class FindCustomPreviewClassVisitor(
  private val customAnnotations: MutableMap<String, CustomPreviewAnnotation>
) : ClassVisitor(Opcodes.ASM9) {

  private val current = CustomPreviewAnnotation()
  private lateinit var className: String

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?,
  ) {
    className = name
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    if (descriptor == PREVIEW_DESCRIPTOR) {
      val config = PreviewConfig()
      current.previewConfigs.add(config)
      return PreviewAnnotationVisitor(config)
    }

    // Inherit configs from already-discovered custom annotations
    if (descriptor != null && customAnnotations.containsKey(descriptor)) {
      current.previewConfigs.addAll(customAnnotations[descriptor]!!.previewConfigs)
    }

    // Check for known built-in multipreview annotations
    val builtinConfigs = builtinMultipreviewConfigs(descriptor)
    if (builtinConfigs != null) {
      current.previewConfigs.addAll(builtinConfigs)
    }

    // Handle @Preview.Container (repeatable @Preview on annotation classes)
    return object : AnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(descriptor, visible)) {
      override fun visitArray(name: String?): AnnotationVisitor? {
        if (name == "value") {
          return object : AnnotationVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
              if (descriptor == PREVIEW_DESCRIPTOR) {
                val config = PreviewConfig()
                current.previewConfigs.add(config)
                return PreviewAnnotationVisitor(config)
              }
              return super.visitAnnotation(name, descriptor)
            }
          }
        }
        return super.visitArray(name)
      }
    }
  }

  override fun visitEnd() {
    customAnnotations["L$className;"] = current
    super.visitEnd()
  }
}

// endregion

// region Second pass: find preview methods

private class PreviewClassVisitor(
  private val includePrivatePreviews: Boolean,
  private val customAnnotations: Map<String, CustomPreviewAnnotation>,
) : ClassVisitor(Opcodes.ASM9) {

  val previewMethods = mutableListOf<PreviewMethod>()
  var sourceFile: String? = null

  override fun visitSource(source: String?, debug: String?) {
    sourceFile = source
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String?,
    signature: String?,
    exceptions: Array<out String>?,
  ): MethodVisitor? {
    if (!includePrivatePreviews && (access and Opcodes.ACC_PRIVATE) != 0) {
      return null
    }
    return PreviewMethodVisitor(name, previewMethods, customAnnotations)
  }
}

private class PreviewMethodVisitor(
  private val methodName: String,
  private val results: MutableList<PreviewMethod>,
  private val customAnnotations: Map<String, CustomPreviewAnnotation>,
) : MethodVisitor(Opcodes.ASM9) {

  private val configs = mutableListOf<PreviewConfig>()
  private var previewParameter: PreviewParameter? = null
  private val parameterNames = mutableMapOf<Int, String>()
  private var parameterCount = 0

  override fun visitParameter(name: String?, access: Int) {
    if (name != null) {
      parameterNames[parameterCount] = name
    }
    parameterCount++
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    if (descriptor == PREVIEW_DESCRIPTOR) {
      val config = PreviewConfig()
      configs.add(config)
      return PreviewAnnotationVisitor(config)
    }

    if (descriptor == PREVIEW_CONTAINER_DESCRIPTOR) {
      return PreviewContainerVisitor(configs)
    }

    val builtinConfigs = builtinMultipreviewConfigs(descriptor)
    if (builtinConfigs != null) {
      configs.addAll(builtinConfigs)
      return null
    }

    if (descriptor != null) {
      val custom = customAnnotations[descriptor]
      if (custom != null && custom.previewConfigs.isNotEmpty()) {
        configs.addAll(custom.previewConfigs.map { it.copy() })
      }
    }

    return null
  }

  override fun visitParameterAnnotation(
    parameter: Int,
    descriptor: String?,
    visible: Boolean,
  ): AnnotationVisitor? {
    if (descriptor == PREVIEW_PARAMETER_DESCRIPTOR) {
      return object : AnnotationVisitor(Opcodes.ASM9) {
        private var providerClassFqn: String? = null
        private var limit: Int? = null
        private var index: Int? = null

        override fun visit(name: String?, value: Any?) {
          when (name) {
            "limit" -> this.limit = value as? Int
            "index" -> this.index = value as? Int
            "provider" -> if (value is Type) providerClassFqn = value.className
          }
        }

        override fun visitEnd() {
          if (providerClassFqn != null) {
            previewParameter =
              PreviewParameter(
                parameterName = parameterNames[parameter] ?: "param$parameter",
                providerClassFqn = providerClassFqn!!,
                limit = limit,
                index = index,
              )
          }
        }
      }
    }
    return super.visitParameterAnnotation(parameter, descriptor, visible)
  }

  override fun visitEnd() {
    for (config in configs) {
      results.add(PreviewMethod(methodName, config, previewParameter))
    }
  }
}

// endregion

// region Annotation visitors

/** Visits the `value` array inside a `@Preview.Container` annotation. */
private class PreviewContainerVisitor(private val configs: MutableList<PreviewConfig>) :
  AnnotationVisitor(Opcodes.ASM9) {

  override fun visitArray(name: String?): AnnotationVisitor? {
    if (name == "value") {
      return object : AnnotationVisitor(Opcodes.ASM9) {
        override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
          if (descriptor == PREVIEW_DESCRIPTOR) {
            val config = PreviewConfig()
            configs.add(config)
            return PreviewAnnotationVisitor(config)
          }
          return super.visitAnnotation(name, descriptor)
        }
      }
    }
    return super.visitArray(name)
  }
}

/** Extracts individual field values from a `@Preview(...)` annotation. */
private class PreviewAnnotationVisitor(
  private val config: PreviewConfig,
  private val onEnd: (() -> Unit)? = null,
) : AnnotationVisitor(Opcodes.ASM9) {

  override fun visit(name: String?, value: Any?) {
    when (name) {
      "apiLevel" -> config.apiLevel = value as? Int
      "locale" -> config.locale = value as? String
      "fontScale" -> config.fontScale = (value as? Float) ?: 1.0f
      "uiMode" -> config.uiMode = (value as? Int) ?: 0
      "showSystemUi" -> config.showSystemUi = (value as? Boolean) ?: false
      "showBackground" -> config.showBackground = (value as? Boolean) ?: false
      "backgroundColor" -> config.backgroundColor = value as? Long
      "name" -> config.name = value as? String
      "group" -> config.group = value as? String
      "device" -> config.device = value as? String
      "widthDp" -> config.widthDp = value as? Int
      "heightDp" -> config.heightDp = value as? Int
      "wallpaper" -> config.wallpaper = value as? Int
    }
  }

  override fun visitEnd() {
    onEnd?.invoke()
  }
}

// endregion

// region Built-in multipreview expansion

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun builtinMultipreviewConfigs(descriptor: String?): List<PreviewConfig>? {
  return when (descriptor) {
    PREVIEW_LIGHT_DARK ->
      listOf(
        PreviewConfig(name = "light"),
        PreviewConfig(name = "dark", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL),
      )

    PREVIEW_FONT_SCALE ->
      listOf(
        PreviewConfig(name = "85%", fontScale = 0.85f),
        PreviewConfig(name = "100%", fontScale = 1.0f),
        PreviewConfig(name = "115%", fontScale = 1.15f),
        PreviewConfig(name = "130%", fontScale = 1.3f),
        PreviewConfig(name = "150%", fontScale = 1.5f),
        PreviewConfig(name = "180%", fontScale = 1.8f),
        PreviewConfig(name = "200%", fontScale = 2f),
      )

    PREVIEW_SCREEN_SIZES ->
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

    PREVIEW_DYNAMIC_COLORS ->
      listOf(
        PreviewConfig(name = "Red", wallpaper = 0),
        PreviewConfig(name = "Blue", wallpaper = 1),
        PreviewConfig(name = "Green", wallpaper = 2),
        PreviewConfig(name = "Yellow", wallpaper = 3),
      )

    WEAR_PREVIEW_SMALL_ROUND ->
      listOf(
        PreviewConfig(
          device = SMALL_ROUND,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Small Round",
          showSystemUi = true,
        )
      )

    WEAR_PREVIEW_LARGE_ROUND ->
      listOf(
        PreviewConfig(
          device = LARGE_ROUND,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Large Round",
          showSystemUi = true,
        )
      )

    WEAR_PREVIEW_SQUARE ->
      listOf(
        PreviewConfig(
          device = SQUARE,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Square",
          showSystemUi = true,
        )
      )

    WEAR_PREVIEW_FONT_SCALES -> {
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

    WEAR_PREVIEW_DEVICES -> {
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

// endregion
