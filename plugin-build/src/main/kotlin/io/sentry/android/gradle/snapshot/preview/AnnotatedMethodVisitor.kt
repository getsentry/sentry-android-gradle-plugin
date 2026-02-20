package io.sentry.android.gradle.snapshot.preview

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor

/**
 * Visits a method's annotations looking for @Preview (direct, container, or custom multi-preview).
 * Collects one [PreviewSnapshotConfig] per preview annotation instance found.
 */
internal class AnnotatedMethodVisitor(
  api: Int,
  private val methodName: String,
  private val className: String,
  private val results: MutableList<PreviewSnapshotConfig>,
  private val customPreviewAnnotations: Map<String, CustomPreviewAnnotation>,
) : MethodVisitor(api) {

  private val previewConfigs = mutableListOf<PreviewConfig>()

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    return when (descriptor) {
      PREVIEW_ANNOTATION_DESC -> {
        val config = PreviewConfig()
        previewConfigs.add(config)
        PreviewAnnotationVisitor(config)
      }
      PREVIEW_CONTAINER_ANNOTATION_DESC -> {
        // Repeatable @Preview stored in a container annotation
        object : AnnotationVisitor(api) {
          override fun visitArray(name: String?): AnnotationVisitor? {
            if (name == "value") {
              return object : AnnotationVisitor(api) {
                override fun visitAnnotation(
                  name: String?,
                  descriptor: String?,
                ): AnnotationVisitor? {
                  if (descriptor == PREVIEW_ANNOTATION_DESC) {
                    val config = PreviewConfig()
                    previewConfigs.add(config)
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
      else -> {
        if (descriptor != null) {
          // Check built-in multi-preview annotations
          val builtinConfigs = previewConfigForAnnotation(descriptor)
          if (builtinConfigs != null) {
            previewConfigs.addAll(builtinConfigs)
          } else {
            // Check user-defined custom multi-preview annotations
            val custom = customPreviewAnnotations[descriptor]
            if (custom != null) {
              previewConfigs.addAll(custom.previewConfigs)
            }
          }
        }
        super.visitAnnotation(descriptor, visible)
      }
    }
  }

  override fun visitEnd() {
    if (previewConfigs.isNotEmpty()) {
      results.addAll(previewConfigs.toSnapshotConfigs())
    }
    super.visitEnd()
  }

  private fun List<PreviewConfig>.toSnapshotConfigs(): List<PreviewSnapshotConfig> {
    val fqClassName = className.replace('/', '.')
    return mapIndexed { index, config ->
      val displayName = buildString {
        append(fqClassName)
        append(".")
        append(methodName)
        if (config.name != null) {
          append(" - ")
          append(config.name)
        } else if (this@toSnapshotConfigs.size > 1) {
          append(" [")
          append(index)
          append("]")
        }
      }
      PreviewSnapshotConfig(
        displayName = displayName,
        className = fqClassName,
        methodName = methodName,
      )
    }
  }
}
