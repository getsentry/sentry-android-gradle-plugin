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
 * classAvailability = new HashMap<>();
 * classAvailability.put("timber.log.Timber", true);
 * ```
 *
 * Known entries avoid reflection; omitted class names still fall back to it. SDK versions without
 * the field are left unchanged.
 */
internal class LoadClassClassVisitor(
  apiVersion: Int,
  nextClassVisitor: ClassVisitor,
  private val classAvailability: Map<String, Boolean>,
) : ClassVisitor(apiVersion, nextClassVisitor) {
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
    visitor.visitTypeInsn(Opcodes.NEW, HASH_MAP_NAME)
    visitor.visitInsn(Opcodes.DUP)
    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, HASH_MAP_NAME, "<init>", "()V", false)
    visitor.visitFieldInsn(
      Opcodes.PUTSTATIC,
      LOAD_CLASS_INTERNAL_NAME,
      AVAILABILITY_FIELD,
      MAP_DESCRIPTOR,
    )

    classAvailability.forEach { (className, available) ->
      visitor.visitFieldInsn(
        Opcodes.GETSTATIC,
        LOAD_CLASS_INTERNAL_NAME,
        AVAILABILITY_FIELD,
        MAP_DESCRIPTOR,
      )
      visitor.visitLdcInsn(className)
      visitor.visitInsn(if (available) Opcodes.ICONST_1 else Opcodes.ICONST_0)
      visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        "java/lang/Boolean",
        "valueOf",
        "(Z)Ljava/lang/Boolean;",
        false,
      )
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

  private companion object {
    const val LOAD_CLASS_INTERNAL_NAME = "io/sentry/util/LoadClass"
    const val AVAILABILITY_FIELD = "classAvailability"
    const val MAP_DESCRIPTOR = "Ljava/util/Map;"
    const val HASH_MAP_NAME = "java/util/HashMap"
    const val STATIC_INITIALIZER = "<clinit>"
    const val VOID_METHOD_DESCRIPTOR = "()V"
  }
}
