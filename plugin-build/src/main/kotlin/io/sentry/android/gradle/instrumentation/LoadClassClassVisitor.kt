package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.SentryPlugin
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Injects build-time knowledge of optional classes into `LoadClass`.
 *
 * `classAvailability` maps class names probed during SDK initialization to whether their owning
 * dependency is present. Before instrumentation the field is uninitialized:
 * ```java
 * static Map<String, Boolean> classAvailability;
 * ```
 *
 * After instrumentation the generated static initializer is equivalent to:
 * ```java
 * classAvailability = SentryGeneratedBuildTimeOptions.getClassAvailability();
 * ```
 *
 * Known entries avoid reflection; omitted class names still fall back to it. SDK versions without
 * the field are left unchanged.
 */
internal class LoadClassClassVisitor(apiVersion: Int, nextClassVisitor: ClassVisitor) :
  ClassVisitor(apiVersion, nextClassVisitor) {
  private var hasAvailabilityField = false
  private var hasStaticInitializer = false

  override fun visitField(
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    value: Any?,
  ): FieldVisitor? {
    if (name == AVAILABILITY_FIELD && descriptor == MAP_DESCRIPTOR) {
      hasAvailabilityField = true
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
    if (name != STATIC_INITIALIZER || descriptor != VOID_METHOD_DESCRIPTOR) {
      return visitor
    }

    hasStaticInitializer = true
    return object : MethodVisitor(api, visitor) {
      override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.RETURN && hasAvailabilityField) {
          injectClassAvailability(this)
        }
        super.visitInsn(opcode)
      }
    }
  }

  override fun visitEnd() {
    if (!hasAvailabilityField) {
      SentryPlugin.logger.info(
        "Sentry SDK runtime reflection checks were not optimized because the current SDK version does not support this optimization."
      )
    }
    if (hasAvailabilityField && !hasStaticInitializer) {
      val visitor =
        super.visitMethod(
          Opcodes.ACC_STATIC,
          STATIC_INITIALIZER,
          VOID_METHOD_DESCRIPTOR,
          null,
          null,
        )
      visitor.visitCode()
      injectClassAvailability(visitor)
      visitor.visitInsn(Opcodes.RETURN)
      // Triggers ASM frame/max computation; arguments are ignored in compute mode.
      visitor.visitMaxs(0, 0)
      visitor.visitEnd()
    }
    super.visitEnd()
  }

  private fun injectClassAvailability(visitor: MethodVisitor) {
    visitor.visitMethodInsn(
      Opcodes.INVOKESTATIC,
      GENERATED_OPTIONS_INTERNAL_NAME,
      "getClassAvailability",
      "()Ljava/util/Map;",
      false,
    )
    visitor.visitFieldInsn(
      Opcodes.PUTSTATIC,
      LOAD_CLASS_INTERNAL_NAME,
      AVAILABILITY_FIELD,
      MAP_DESCRIPTOR,
    )
  }

  private companion object {
    const val LOAD_CLASS_INTERNAL_NAME = "io/sentry/util/LoadClass"
    const val GENERATED_OPTIONS_INTERNAL_NAME =
      "io/sentry/android/core/SentryGeneratedBuildTimeOptions"
    const val AVAILABILITY_FIELD = "classAvailability"
    const val MAP_DESCRIPTOR = "Ljava/util/Map;"
    const val STATIC_INITIALIZER = "<clinit>"
    const val VOID_METHOD_DESCRIPTOR = "()V"
  }
}
