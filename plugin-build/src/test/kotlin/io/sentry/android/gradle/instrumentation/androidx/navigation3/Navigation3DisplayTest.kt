package io.sentry.android.gradle.instrumentation.androidx.navigation3

import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor.RememberDecoratedNavEntriesRedirectingVisitor.Companion.DESCRIPTOR
import io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor.RememberDecoratedNavEntriesRedirectingVisitor.Companion.ORIGINAL_NAME
import io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor.RememberDecoratedNavEntriesRedirectingVisitor.Companion.ORIGINAL_OWNER
import io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor.RememberDecoratedNavEntriesRedirectingVisitor.Companion.REPLACEMENT_NAME
import io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor.RememberDecoratedNavEntriesRedirectingVisitor.Companion.REPLACEMENT_OWNER
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

@RunWith(Parameterized::class)
class Navigation3DisplayTest(private val navigation3Version: String) {

  @get:Rule val tmpDir = TemporaryFolder()

  private val instrumentable = Navigation3Display()

  @Test
  fun `fixture calls rememberDecoratedNavEntries exactly once`() {
    // Guards the fixture itself: if this fails, the assertions below prove nothing.
    val calls = collectStaticCalls(loadFixture())

    assertThat(calls.count { it.isOriginal() }).isEqualTo(1)
    assertThat(calls.count { it.isReplacement() }).isEqualTo(0)
  }

  @Test
  fun `redirects the rememberDecoratedNavEntries call to the Sentry wrapper`() {
    val calls = collectStaticCalls(instrument(loadFixture()))

    assertThat(calls.count { it.isOriginal() }).isEqualTo(0)
    assertThat(calls.count { it.isReplacement() }).isEqualTo(1)
  }

  @Test
  fun `leaves all other calls unchanged`() {
    val original = collectStaticCalls(loadFixture()).filterNot { it.isOriginal() }
    val instrumented =
      collectStaticCalls(instrument(loadFixture())).filterNot { it.isReplacement() }

    assertThat(instrumented).containsExactlyElementsIn(original).inOrder()
  }

  private fun loadFixture(): ByteArray =
    File(
        "src/test/resources/testFixtures/instrumentation/androidxNavigation3/" +
          "$navigation3Version/NavDisplayKt__NavDisplayKt.class"
      )
      .readBytes()

  private fun instrument(bytes: ByteArray): ByteArray {
    val reader = ClassReader(bytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    val visitor =
      instrumentable.getVisitor(
        TestClassContext(Navigation3Display.NAV_DISPLAY_CLASSNAME),
        Opcodes.ASM9,
        writer,
        TestSpanAddingParameters(debugOutput = false, inMemoryDir = tmpDir.root),
      )
    reader.accept(visitor, ClassReader.SKIP_FRAMES)
    return writer.toByteArray()
  }

  companion object {
    @Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = listOf("1_0_1", "1_2_0", "1_3_0_alpha03")
  }
}

class Navigation3DisplayUnitTest {

  @get:Rule val tmpDir = TemporaryFolder()

  private val instrumentable = Navigation3Display()

  @Test
  fun `isInstrumentable only matches NavDisplay's implementation class`() {
    assertThat(
        instrumentable.isInstrumentable(
          TestClassContext("androidx.navigation3.ui.NavDisplayKt__NavDisplayKt")
        )
      )
      .isTrue()
    assertThat(
        instrumentable.isInstrumentable(TestClassContext("androidx.navigation3.ui.NavDisplayKt"))
      )
      .isFalse()
    assertThat(
        instrumentable.isInstrumentable(
          TestClassContext("androidx.navigation3.runtime.DecoratedNavEntriesKt")
        )
      )
      .isFalse()
    assertThat(instrumentable.isInstrumentable(TestClassContext("com.example.MainActivityKt")))
      .isFalse()
  }

  @Test
  fun `does not redirect calls with other owners, names, or descriptors`() {
    val entriesOverloadDescriptor =
      "(Ljava/util/List;Ljava/util/List;Landroidx/compose/runtime/Composer;II)Ljava/util/List;"
    val bytes =
      generateClass(
        StaticCall(ORIGINAL_OWNER, ORIGINAL_NAME, entriesOverloadDescriptor),
        StaticCall("com/example/OtherKt", ORIGINAL_NAME, DESCRIPTOR),
        StaticCall(ORIGINAL_OWNER, "rememberSomethingElse", DESCRIPTOR),
      )

    val calls = collectStaticCalls(instrument(bytes))

    assertThat(calls)
      .containsExactly(
        StaticCall(ORIGINAL_OWNER, ORIGINAL_NAME, entriesOverloadDescriptor),
        StaticCall("com/example/OtherKt", ORIGINAL_NAME, DESCRIPTOR),
        StaticCall(ORIGINAL_OWNER, "rememberSomethingElse", DESCRIPTOR),
      )
      .inOrder()
  }

  @Test
  fun `redirects matching calls in any method of the class`() {
    val bytes =
      generateClass(
        StaticCall(ORIGINAL_OWNER, ORIGINAL_NAME, DESCRIPTOR),
        methodName = "someHelper",
      )

    val calls = collectStaticCalls(instrument(bytes))

    assertThat(calls).containsExactly(StaticCall(REPLACEMENT_OWNER, REPLACEMENT_NAME, DESCRIPTOR))
  }

  @Test
  fun `keeps the replacement descriptor identical to the original`() {
    // The wrapper in sentry-android-navigation3 must keep this exact signature.
    assertThat(Type.getArgumentTypes(DESCRIPTOR).map { it.descriptor })
      .containsExactly(
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Lkotlin/jvm/functions/Function1;",
        "Landroidx/compose/runtime/Composer;",
        "I",
        "I",
      )
      .inOrder()
    assertThat(Type.getReturnType(DESCRIPTOR).descriptor).isEqualTo("Ljava/util/List;")
  }

  private fun instrument(bytes: ByteArray): ByteArray {
    val reader = ClassReader(bytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    val visitor =
      instrumentable.getVisitor(
        TestClassContext(Navigation3Display.NAV_DISPLAY_CLASSNAME),
        Opcodes.ASM9,
        writer,
        TestSpanAddingParameters(debugOutput = false, inMemoryDir = tmpDir.root),
      )
    reader.accept(visitor, ClassReader.SKIP_FRAMES)
    return writer.toByteArray()
  }

  /**
   * Generates a class with one static method that makes each of [calls] with `null`/zero arguments
   * and discards the results.
   */
  private fun generateClass(
    vararg calls: StaticCall,
    methodName: String = "NavDisplay",
  ): ByteArray {
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    writer.visit(
      Opcodes.V1_8,
      Opcodes.ACC_PUBLIC,
      "androidx/navigation3/ui/NavDisplayKt__NavDisplayKt",
      null,
      "java/lang/Object",
      null,
    )
    val mv =
      writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, methodName, "()V", null, null)
    mv.visitCode()
    calls.forEach { call ->
      Type.getArgumentTypes(call.descriptor).forEach { type ->
        if (type.sort == Type.OBJECT) mv.visitInsn(Opcodes.ACONST_NULL)
        else mv.visitInsn(Opcodes.ICONST_0)
      }
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, call.owner, call.name, call.descriptor, false)
      mv.visitInsn(Opcodes.POP)
    }
    mv.visitInsn(Opcodes.RETURN)
    mv.visitMaxs(0, 0)
    mv.visitEnd()
    writer.visitEnd()
    return writer.toByteArray()
  }
}

private data class StaticCall(val owner: String, val name: String, val descriptor: String) {
  fun isOriginal(): Boolean =
    owner == ORIGINAL_OWNER && name == ORIGINAL_NAME && descriptor == DESCRIPTOR

  fun isReplacement(): Boolean =
    owner == REPLACEMENT_OWNER && name == REPLACEMENT_NAME && descriptor == DESCRIPTOR
}

private fun collectStaticCalls(bytes: ByteArray): List<StaticCall> {
  val calls = mutableListOf<StaticCall>()
  ClassReader(bytes)
    .accept(
      object : ClassVisitor(Opcodes.ASM9) {
        override fun visitMethod(
          access: Int,
          name: String?,
          descriptor: String?,
          signature: String?,
          exceptions: Array<out String>?,
        ): MethodVisitor =
          object : MethodVisitor(Opcodes.ASM9) {
            override fun visitMethodInsn(
              opcode: Int,
              owner: String,
              name: String,
              descriptor: String,
              isInterface: Boolean,
            ) {
              if (opcode == Opcodes.INVOKESTATIC) calls += StaticCall(owner, name, descriptor)
            }
          }
      },
      0,
    )
  return calls
}
