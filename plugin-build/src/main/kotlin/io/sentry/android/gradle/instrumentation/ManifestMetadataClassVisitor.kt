package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.SentryPlugin
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/** Injects fully resolved build-time manifest metadata into `ManifestMetadataReader`. */
internal class ManifestMetadataClassVisitor(
  apiVersion: Int,
  nextClassVisitor: ClassVisitor,
  private val metadata: Map<String, Any>,
) : ClassVisitor(apiVersion, nextClassVisitor) {
  private var hasMetadataField = false
  private var hasStaticInitializer = false

  override fun visitField(
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    value: Any?,
  ): FieldVisitor? {
    if (name == METADATA_FIELD && descriptor == MAP_DESCRIPTOR) hasMetadataField = true
    return super.visitField(access, name, descriptor, signature, value)
  }

  override fun visitMethod(
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    exceptions: Array<out String>?,
  ): MethodVisitor {
    val visitor = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name != STATIC_INITIALIZER || descriptor != VOID_METHOD_DESCRIPTOR) return visitor

    hasStaticInitializer = true
    return object : MethodVisitor(api, visitor) {
      override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.RETURN && hasMetadataField) injectMetadata(this)
        super.visitInsn(opcode)
      }
    }
  }

  override fun visitEnd() {
    if (!hasMetadataField) {
      SentryPlugin.logger.info(
        "Sentry manifest metadata was not optimized because the current SDK version does not support this optimization."
      )
    }
    if (hasMetadataField && !hasStaticInitializer) {
      val visitor =
        super.visitMethod(
          Opcodes.ACC_STATIC,
          STATIC_INITIALIZER,
          VOID_METHOD_DESCRIPTOR,
          null,
          null,
        )
      visitor.visitCode()
      injectMetadata(visitor)
      visitor.visitInsn(Opcodes.RETURN)
      visitor.visitMaxs(0, 0)
      visitor.visitEnd()
    }
    super.visitEnd()
  }

  private fun injectMetadata(visitor: MethodVisitor) {
    visitor.visitTypeInsn(Opcodes.NEW, HASH_MAP_NAME)
    visitor.visitInsn(Opcodes.DUP)
    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, HASH_MAP_NAME, "<init>", "()V", false)
    visitor.visitFieldInsn(Opcodes.PUTSTATIC, READER_INTERNAL_NAME, METADATA_FIELD, MAP_DESCRIPTOR)

    metadata.forEach { (key, value) ->
      visitor.visitFieldInsn(
        Opcodes.GETSTATIC,
        READER_INTERNAL_NAME,
        METADATA_FIELD,
        MAP_DESCRIPTOR,
      )
      visitor.visitLdcInsn(key)
      visitor.emitValue(value)
      visitor.visitMethodInsn(
        Opcodes.INVOKEINTERFACE,
        "java/util/Map",
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
        true,
      )
      visitor.visitInsn(Opcodes.POP)
    }
  }

  private fun MethodVisitor.emitValue(value: Any) {
    when (value) {
      is Boolean -> {
        visitInsn(if (value) Opcodes.ICONST_1 else Opcodes.ICONST_0)
        visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "java/lang/Boolean",
          "valueOf",
          "(Z)Ljava/lang/Boolean;",
          false,
        )
      }
      is Int -> {
        visitLdcInsn(value)
        visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "java/lang/Integer",
          "valueOf",
          "(I)Ljava/lang/Integer;",
          false,
        )
      }
      is Float -> {
        visitLdcInsn(value)
        visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "java/lang/Float",
          "valueOf",
          "(F)Ljava/lang/Float;",
          false,
        )
      }
      is String -> visitLdcInsn(value)
    }
  }

  private companion object {
    const val READER_INTERNAL_NAME = "io/sentry/android/core/ManifestMetadataReader"
    const val METADATA_FIELD = "buildTimeMetadata"
    const val MAP_DESCRIPTOR = "Ljava/util/Map;"
    const val HASH_MAP_NAME = "java/util/HashMap"
    const val STATIC_INITIALIZER = "<clinit>"
    const val VOID_METHOD_DESCRIPTOR = "()V"
  }
}
