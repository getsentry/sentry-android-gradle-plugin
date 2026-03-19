package io.sentry.android.gradle.snapshot.metadata

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

private const val PREVIEW_DESCRIPTOR = "Landroidx/compose/ui/tooling/preview/Preview;"

data class PreviewConfig(
  var apiLevel: Int? = null,
  var locale: String? = null,
  var fontScale: Float = 1.0f,
  var uiMode: Int = 0,
  var showSystemUi: Boolean = false,
  var showBackground: Boolean = false,
  var backgroundColor: Long? = null,
  var name: String? = null,
  var device: String? = null,
  var widthDp: Int? = null,
  var heightDp: Int? = null,
)

data class PreviewMethod(val methodName: String, val config: PreviewConfig)

class PreviewMethodScanner(private val includePrivatePreviews: Boolean) {

  fun scan(classBytes: ByteArray): List<PreviewMethod> {
    val reader = ClassReader(classBytes)
    val visitor = PreviewClassVisitor(includePrivatePreviews)
    reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
    return visitor.previewMethods
  }
}

private class PreviewClassVisitor(private val includePrivatePreviews: Boolean) :
  ClassVisitor(Opcodes.ASM9) {

  val previewMethods = mutableListOf<PreviewMethod>()

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
    return PreviewMethodVisitor(name, previewMethods)
  }
}

private class PreviewMethodVisitor(
  private val methodName: String,
  private val results: MutableList<PreviewMethod>,
) : MethodVisitor(Opcodes.ASM9) {

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    if (descriptor == PREVIEW_DESCRIPTOR) {
      val config = PreviewConfig()
      return PreviewAnnotationVisitor(config) { results.add(PreviewMethod(methodName, config)) }
    }
    return null
  }
}

private class PreviewAnnotationVisitor(
  private val config: PreviewConfig,
  private val onEnd: () -> Unit,
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
      "device" -> config.device = value as? String
      "widthDp" -> config.widthDp = value as? Int
      "heightDp" -> config.heightDp = value as? Int
    }
  }

  override fun visitEnd() {
    onEnd()
  }
}
