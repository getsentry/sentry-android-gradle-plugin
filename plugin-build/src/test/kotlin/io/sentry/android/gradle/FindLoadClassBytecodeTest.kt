package io.sentry.android.gradle

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.instrumentation.LoadClassClassVisitor
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class FindLoadClassBytecodeTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `prefers instrumented LoadClass over uninstrumented walk order`() {
    val root = tempDir.newFolder("app-build")

    // Uninstrumented copy first in walk order (plain class file under a non-asm path).
    val uninstrumentedDir =
      root.resolve("intermediates/runtime_library_classes_dir/debug/io/sentry/util").apply {
        mkdirs()
      }
    uninstrumentedDir.resolve("LoadClass.class").writeBytes(originalLoadClassBytes())

    // Instrumented copy later, nested under an asm-looking path + jar.
    val instrumentedJarDir =
      root.resolve("intermediates/asm_instrumented_jars/debug").apply { mkdirs() }
    val instrumentedBytes =
      instrumentedLoadClassBytes(
        mapOf("timber.log.Timber" to true, "androidx.compose.ui.node.Owner" to false)
      )
    writeJarWithEntry(
      instrumentedJarDir.resolve("sentry-android-core.jar"),
      "io/sentry/util/LoadClass.class",
      instrumentedBytes,
    )

    val found = findLoadClassBytecode(root)
    assertThat(hasClassAvailabilityInjection(found)).isTrue()
    assertThat(readInjectedAvailability(found))
      .containsAtLeastEntriesIn(
        mapOf("timber.log.Timber" to true, "androidx.compose.ui.node.Owner" to false)
      )
  }

  @Test
  fun `errors when only uninstrumented LoadClass is present`() {
    val root = tempDir.newFolder("only-uninstrumented")
    val dir = root.resolve("io/sentry/util").apply { mkdirs() }
    dir.resolve("LoadClass.class").writeBytes(originalLoadClassBytes())

    val error = runCatching { findLoadClassBytecode(root) }.exceptionOrNull()
    assertThat(error).isInstanceOf(IllegalStateException::class.java)
    assertThat(error!!.message).contains("Candidates without injection marker")
  }

  private fun originalLoadClassBytes(): ByteArray {
    val writer = ClassWriter(0)
    writer.visit(
      Opcodes.V1_8,
      Opcodes.ACC_PUBLIC,
      "io/sentry/util/LoadClass",
      null,
      "java/lang/Object",
      null,
    )
    writer
      .visitField(
        Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
        "classAvailability",
        "Ljava/util/Map;",
        null,
        null,
      )
      .visitEnd()
    writer.visitEnd()
    return writer.toByteArray()
  }

  private fun instrumentedLoadClassBytes(availability: Map<String, Boolean>): ByteArray {
    val reader = ClassReader(originalLoadClassBytes())
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
    reader.accept(LoadClassClassVisitor(Opcodes.ASM9, writer, availability), 0)
    return writer.toByteArray()
  }

  private fun writeJarWithEntry(jar: java.io.File, entryName: String, bytes: ByteArray) {
    JarOutputStream(jar.outputStream()).use { jos ->
      jos.putNextEntry(JarEntry(entryName))
      jos.write(bytes)
      jos.closeEntry()
    }
  }
}
