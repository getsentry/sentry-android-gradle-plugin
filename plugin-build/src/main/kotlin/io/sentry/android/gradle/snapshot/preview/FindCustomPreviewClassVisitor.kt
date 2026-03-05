package io.sentry.android.gradle.snapshot.preview

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/**
 * First-pass visitor that identifies annotation classes meta-annotated with @Preview. These are
 * custom multi-preview annotations (e.g. @PreviewLightDark or user-defined ones).
 */
internal class FindCustomPreviewClassVisitor(
  private val customPreviewAnnotations: MutableMap<String, CustomPreviewAnnotation>
) : ClassVisitor(Opcodes.ASM9) {

  private val currentAnnotation = CustomPreviewAnnotation()
  private lateinit var currentClassName: String

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?,
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    currentClassName = name
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    if (descriptor == PREVIEW_ANNOTATION_DESC) {
      val config = PreviewConfig()
      currentAnnotation.previewConfigs.add(config)
      return PreviewAnnotationVisitor(config)
    }

    if (customPreviewAnnotations.containsKey(descriptor)) {
      val source = customPreviewAnnotations[descriptor]!!
      currentAnnotation.previewConfigs.addAll(source.previewConfigs)
    }

    val builtinConfigs = previewConfigForAnnotation(descriptor)
    if (builtinConfigs != null) {
      currentAnnotation.previewConfigs.addAll(builtinConfigs)
    }

    return object : AnnotationVisitor(api, super.visitAnnotation(descriptor, visible)) {
      override fun visitArray(name: String?): AnnotationVisitor? {
        if (name == "value") {
          return object : AnnotationVisitor(api) {
            override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
              if (descriptor == PREVIEW_ANNOTATION_DESC) {
                val config = PreviewConfig()
                currentAnnotation.previewConfigs.add(config)
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
    val descriptor = "L$currentClassName;"
    customPreviewAnnotations[descriptor] = currentAnnotation
    super.visitEnd()
  }
}
