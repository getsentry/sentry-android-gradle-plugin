package io.sentry.android.gradle.instrumentation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class ManifestMetadataClassVisitorTest {
  @Test
  fun `injects typed metadata when static initializer is missing`() {
    val metadata =
      mapOf(
        "io.sentry.debug" to true,
        "io.sentry.max-breadcrumbs" to 42,
        "io.sentry.sample-rate" to 0.5f,
        "io.sentry.dsn" to "https://example.invalid/1",
      )

    val clazz = load(transformClass(metadata = metadata))

    assertThat(readMetadata(clazz)).containsExactlyEntriesIn(metadata)
  }

  @Test
  fun `injects metadata and preserves existing static initializer`() {
    val metadata = mapOf("io.sentry.debug" to false)

    val clazz = load(transformClass(hasStaticInitializer = true, metadata = metadata))

    assertThat(clazz.getDeclaredField("marker").getInt(null)).isEqualTo(7)
    assertThat(readMetadata(clazz)).containsExactlyEntriesIn(metadata)
  }

  @Test
  fun `does not modify SDK versions without the metadata field`() {
    val clazz = load(transformClass(hasMetadataField = false))

    assertThat(clazz.declaredFields.map { it.name }).doesNotContain("buildTimeMetadata")
  }

  private fun transformClass(
    hasMetadataField: Boolean = true,
    hasStaticInitializer: Boolean = false,
    metadata: Map<String, Any> = emptyMap(),
  ): ByteArray {
    val original = ClassWriter(0)
    original.visit(
      Opcodes.V1_8,
      Opcodes.ACC_PUBLIC,
      READER_INTERNAL_NAME,
      null,
      "java/lang/Object",
      null,
    )
    if (hasMetadataField) {
      original
        .visitField(Opcodes.ACC_STATIC, "buildTimeMetadata", "Ljava/util/Map;", null, null)
        .visitEnd()
    }
    if (hasStaticInitializer) {
      original
        .visitField(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "marker", "I", null, null)
        .visitEnd()
      original.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null).apply {
        visitCode()
        visitIntInsn(Opcodes.BIPUSH, 7)
        visitFieldInsn(Opcodes.PUTSTATIC, READER_INTERNAL_NAME, "marker", "I")
        visitInsn(Opcodes.RETURN)
        visitMaxs(1, 0)
        visitEnd()
      }
    }
    original.visitEnd()

    val reader = ClassReader(original.toByteArray())
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
    reader.accept(ManifestMetadataClassVisitor(Opcodes.ASM9, writer, metadata), 0)
    return writer.toByteArray()
  }

  private fun load(bytes: ByteArray): Class<*> =
    // Loading the transformed bytes in an isolated class loader executes their generated <clinit>.
    object : ClassLoader(javaClass.classLoader) {
        fun define(): Class<*> =
          defineClass(
            SentrySdkOptimizationClassVisitorFactory.MANIFEST_METADATA_READER_NAME,
            bytes,
            0,
            bytes.size,
          )
      }
      .define()

  @Suppress("UNCHECKED_CAST")
  private fun readMetadata(clazz: Class<*>): Map<String, Any> =
    clazz.getDeclaredField("buildTimeMetadata").run {
      isAccessible = true
      get(null) as Map<String, Any>
    }

  private companion object {
    const val READER_INTERNAL_NAME = "io/sentry/android/core/ManifestMetadataReader"
  }
}
