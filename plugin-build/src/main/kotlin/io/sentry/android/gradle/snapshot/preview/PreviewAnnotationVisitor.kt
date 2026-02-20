package io.sentry.android.gradle.snapshot.preview

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes

/** Captures @Preview annotation parameters into a [PreviewConfig]. */
internal class PreviewAnnotationVisitor(private val previewConfig: PreviewConfig) :
  AnnotationVisitor(Opcodes.ASM9) {

  override fun visit(name: String?, value: Any?) {
    when (name) {
      "name" -> if (value is String) previewConfig.name = value
      "group" -> if (value is String) previewConfig.group = value
      "uiMode" -> if (value is Int) previewConfig.uiMode = value
      "locale" -> if (value is String) previewConfig.locale = value
      "fontScale" -> if (value is Float) previewConfig.fontScale = value
      "heightDp" -> if (value is Int) previewConfig.heightDp = value
      "widthDp" -> if (value is Int) previewConfig.widthDp = value
      "showBackground" -> if (value is Boolean) previewConfig.showBackground = value
      "backgroundColor" -> if (value is Long) previewConfig.backgroundColor = value
      "showSystemUi" -> if (value is Boolean) previewConfig.showSystemUi = value
      "device" -> if (value is String) previewConfig.device = value
      "apiLevel" -> if (value is Int) previewConfig.apiLevel = value
      "wallpaper" -> if (value is Int) previewConfig.wallpaper = value
    }
    super.visit(name, value)
  }
}
