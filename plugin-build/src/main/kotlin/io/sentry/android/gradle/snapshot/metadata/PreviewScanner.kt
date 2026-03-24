package io.sentry.android.gradle.snapshot.metadata

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

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
