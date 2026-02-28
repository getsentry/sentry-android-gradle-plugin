package io.sentry.android.gradle.snapshot.preview

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Second-pass class visitor that delegates to [AnnotatedMethodVisitor] for each method, collecting
 * preview snapshot configs.
 */
internal class SnapshotClassVisitor(
  api: Int,
  private val className: String,
  private val results: MutableList<PreviewSnapshotConfig>,
  private val includePrivatePreviews: Boolean,
  private val customPreviewAnnotations: Map<String, CustomPreviewAnnotation>,
) : ClassVisitor(api) {

  override fun visitMethod(
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<out String>?,
  ): MethodVisitor? {
    if (!includePrivatePreviews && (access and Opcodes.ACC_PRIVATE) != 0) {
      return super.visitMethod(access, name, desc, signature, exceptions)
    }
    return AnnotatedMethodVisitor(api, name, className, results, customPreviewAnnotations)
  }
}
