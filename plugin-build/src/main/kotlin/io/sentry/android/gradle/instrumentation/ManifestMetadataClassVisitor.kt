package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.SentryPlugin
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/** Injects app-owned manifest metadata into the SDK's manifest metadata reader. */
internal class ManifestMetadataClassVisitor(apiVersion: Int, nextClassVisitor: ClassVisitor) :
  ClassVisitor(apiVersion, nextClassVisitor) {
  private var hasMetadataField = false
  private var hasStaticInitializer = false

  override fun visitField(
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    value: Any?,
  ): FieldVisitor? {
    if (
      name == METADATA_FIELD && descriptor == MAP_DESCRIPTOR && access and Opcodes.ACC_STATIC != 0
    ) {
      hasMetadataField = true
    }
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
    visitor.visitMethodInsn(
      Opcodes.INVOKESTATIC,
      GENERATED_OPTIONS_INTERNAL_NAME,
      "getManifestMetadata",
      "()Ljava/util/Map;",
      false,
    )
    visitor.visitFieldInsn(
      Opcodes.PUTSTATIC,
      MANIFEST_METADATA_READER_INTERNAL_NAME,
      METADATA_FIELD,
      MAP_DESCRIPTOR,
    )
  }

  private companion object {
    const val MANIFEST_METADATA_READER_INTERNAL_NAME =
      "io/sentry/android/core/ManifestMetadataReader"
    const val GENERATED_OPTIONS_INTERNAL_NAME =
      "io/sentry/android/core/SentryGeneratedBuildTimeOptions"
    const val METADATA_FIELD = "manifestMetadata"
    const val MAP_DESCRIPTOR = "Ljava/util/Map;"
    const val STATIC_INITIALIZER = "<clinit>"
    const val VOID_METHOD_DESCRIPTOR = "()V"
  }
}
