package io.sentry.android.gradle.instrumentation

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.tasks.dependencies.ResolveSdkClassAvailabilityTask
import io.sentry.android.gradle.util.SentryModules
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class LoadClassClassVisitorTest {

  @get:Rule val tempDir = TemporaryFolder()

  @Test
  fun `resolves every known class from the module graph`() {
    val modules = SentrySdkOptimizationClassVisitorFactory.CLASS_MODULES.values.flatten().toSet()

    assertThat(resolveClassAvailability(modules).values).doesNotContain(false)
  }

  @Test
  fun `marks every known class as unavailable when its module is absent`() {
    val availability = resolveClassAvailability(emptySet())

    assertThat(availability).hasSize(SentrySdkOptimizationClassVisitorFactory.CLASS_MODULES.size)
    assertThat(availability.values).doesNotContain(true)
  }

  @Test
  fun `supports platform-specific module coordinates`() {
    val modules =
      setOf(
        DefaultModuleIdentifier.newId("androidx.compose.ui", "ui-android"),
        DefaultModuleIdentifier.newId("androidx.lifecycle", "lifecycle-common-jvm"),
        SentryModules.SENTRY_ANDROID_REPLAY,
      )

    assertThat(resolveClassAvailability(modules))
      .containsAtLeast(
        "androidx.compose.ui.node.Owner",
        true,
        "androidx.lifecycle.Lifecycle",
        true,
        "io.sentry.android.replay.ReplayIntegration",
        true,
      )
  }

  @Test
  fun `injects availability map when static initializer is missing`() {
    val availability = mapOf("present.Class" to true, "missing.Class" to false)

    val clazz = load(transformClass(hasStaticInitializer = false, availability = availability))

    assertThat(readAvailability(clazz)).containsExactlyEntriesIn(availability)
  }

  @Test
  fun `injects availability map and preserves existing static initializer`() {
    val availability = mapOf("present.Class" to true)

    val clazz = load(transformClass(hasStaticInitializer = true, availability = availability))

    assertThat(clazz.getDeclaredField("marker").getInt(null)).isEqualTo(7)
    assertThat(readAvailability(clazz)).containsExactlyEntriesIn(availability)
  }

  @Test
  fun `does not modify SDK versions without the availability field`() {
    val clazz = load(transformClass(hasAvailabilityField = false))

    assertThat(clazz.declaredFields.map { it.name }).doesNotContain("classAvailability")
  }

  @Test
  fun `task file round-trips into injected LoadClass availability`() {
    val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder("project")).build()
    val output = tempDir.newFile("availability.properties")
    val task =
      project.tasks.register(
        "glueResolveSdkClassAvailability",
        ResolveSdkClassAvailabilityTask::class.java,
      ) {
        it.moduleIds.set(setOf("com.jakewharton.timber:timber", "androidx.core:core"))
        it.outputFile.set(output)
      }

    task.get().action()

    val availability = readClassAvailability(task.get().outputFile)
    val clazz = load(transformClass(availability = availability))

    assertThat(readAvailability(clazz))
      .containsAtLeast(
        "timber.log.Timber",
        true,
        "androidx.core.view.ScrollingView",
        true,
        "androidx.compose.ui.node.Owner",
        false,
      )
  }

  private fun transformClass(
    hasAvailabilityField: Boolean = true,
    hasStaticInitializer: Boolean = false,
    availability: Map<String, Boolean> = emptyMap(),
  ): ByteArray {
    val original = ClassWriter(0)
    original.visit(
      Opcodes.V1_8,
      Opcodes.ACC_PUBLIC,
      LOAD_CLASS_INTERNAL_NAME,
      null,
      "java/lang/Object",
      null,
    )
    if (hasAvailabilityField) {
      original
        .visitField(
          Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
          "classAvailability",
          "Ljava/util/Map;",
          null,
          null,
        )
        .visitEnd()
    }
    if (hasStaticInitializer) {
      original
        .visitField(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "marker", "I", null, null)
        .visitEnd()
      original.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null).apply {
        visitCode()
        visitIntInsn(Opcodes.BIPUSH, 7)
        visitFieldInsn(Opcodes.PUTSTATIC, LOAD_CLASS_INTERNAL_NAME, "marker", "I")
        visitInsn(Opcodes.RETURN)
        visitMaxs(1, 0)
        visitEnd()
      }
    }
    original.visitEnd()

    val reader = ClassReader(original.toByteArray())
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
    reader.accept(LoadClassClassVisitor(Opcodes.ASM9, writer, availability), 0)
    return writer.toByteArray()
  }

  private fun load(bytes: ByteArray): Class<*> =
    object : ClassLoader(javaClass.classLoader) {
        fun define(): Class<*> =
          defineClass(
            SentrySdkOptimizationClassVisitorFactory.LOAD_CLASS_NAME,
            bytes,
            0,
            bytes.size,
          )
      }
      .define()

  @Suppress("UNCHECKED_CAST")
  private fun readAvailability(clazz: Class<*>): Map<String, Boolean> =
    clazz.getDeclaredField("classAvailability").run {
      isAccessible = true
      get(null) as Map<String, Boolean>
    }

  private companion object {
    const val LOAD_CLASS_INTERNAL_NAME = "io/sentry/util/LoadClass"
  }
}
