package io.sentry.android.gradle.instrumentation.wrap.visitor

import com.android.build.api.instrumentation.ClassData
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.instrumentation.fakes.TestClassData
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

class WrappingVisitorTest {

  class Fixture {
    val logger = CapturingTestLogger()
    val visitor = CapturingMethodVisitor()

    fun getSut(
      methodContext: MethodContext = MethodContext(Opcodes.ACC_PUBLIC, "test", "()V", null, null),
      classContext: ClassData = TestClassData("io/sentry/RandomClass"),
      replacements: Map<Replacement, Replacement> = mapOf(),
      firstPassVisitor: MethodNode = MethodNode(Opcodes.ASM7),
    ) =
      WrappingVisitor(
        Opcodes.ASM7,
        visitor,
        firstPassVisitor,
        classContext,
        methodContext,
        replacements,
        logger,
      )
  }

  private val fixture = Fixture()

  @Test
  fun `invokedynamic is skipped from instrumentation`() {
    val context = MethodContext(Opcodes.ACC_PUBLIC, "test", "()V", null, null)
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKEDYNAMIC,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    fixture
      .getSut(context)
      .visitMethodInsn(
        opcode = methodVisit.opcode,
        owner = methodVisit.owner,
        name = methodVisit.name,
        descriptor = methodVisit.descriptor,
        isInterface = methodVisit.isInterface,
      )

    assertEquals(
      fixture.logger.capturedMessage,
      "[sentry] INVOKEDYNAMIC skipped from instrumentation for io.sentry.RandomClass.test",
    )
    // method visit should remain unchanged
    assertEquals(fixture.visitor.methodVisits.size, 1)
    assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
  }

  @Test
  fun `when no replacements found does not modify method visit`() {
    val context = MethodContext(Opcodes.ACC_PUBLIC, "test", "()V", null, null)
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKEVIRTUAL,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    fixture
      .getSut(context)
      .visitMethodInsn(
        opcode = methodVisit.opcode,
        owner = methodVisit.owner,
        name = methodVisit.name,
        descriptor = methodVisit.descriptor,
        isInterface = methodVisit.isInterface,
      )

    // method visit should remain unchanged
    assertEquals(fixture.visitor.methodVisits.size, 1)
    assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
  }

  @Test
  fun `when replacement found and super call in override does not modify method visit`() {
    val context =
      MethodContext(
        Opcodes.ACC_PUBLIC,
        "openFileInput",
        "(Ljava/lang/String;)Ljava/io/FileInputStream;",
        null,
        null,
      )
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKESPECIAL,
        owner = "android/content/Context",
        name = "openFileInput",
        descriptor = "(Ljava/lang/String;)Ljava/io/FileInputStream;",
        isInterface = false,
      )
    fixture
      .getSut(
        context,
        classContext = TestClassData("io/sentry/android/sample/LyricsActivity"),
        replacements = mapOf(Replacement.Context.OPEN_FILE_INPUT),
      )
      .visitMethodInsn(
        opcode = methodVisit.opcode,
        owner = methodVisit.owner,
        name = methodVisit.name,
        descriptor = methodVisit.descriptor,
        isInterface = methodVisit.isInterface,
      )

    assertEquals(
      fixture.logger.capturedMessage,
      "[sentry] io.sentry.android.sample.LyricsActivity skipped from instrumentation " +
        "in overridden method openFileInput.(Ljava/lang/String;)Ljava/io/FileInputStream;",
    )
    // method visit should remain unchanged
    assertEquals(fixture.visitor.methodVisits.size, 1)
    assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
  }

  @Test
  fun `when replacement found and super call in constructor does not modify method visit`() {
    val context = MethodContext(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKESPECIAL,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    fixture
      .getSut(
        context,
        classContext =
          TestClassData(
            "io/sentry/CustomFileInputStream",
            superClasses = listOf("java/io/FileInputStream"),
          ),
        replacements = mapOf(Replacement.FileInputStream.STRING),
      )
      .visitMethodInsn(
        opcode = methodVisit.opcode,
        owner = methodVisit.owner,
        name = methodVisit.name,
        descriptor = methodVisit.descriptor,
        isInterface = methodVisit.isInterface,
      )

    assertEquals(
      fixture.logger.capturedMessage,
      "[sentry] io.sentry.CustomFileInputStream skipped from instrumentation in " +
        "constructor <init>.(Ljava/lang/String;)V",
    )
    // method visit should remain unchanged
    assertEquals(fixture.visitor.methodVisits.size, 1)
    assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
  }

  @Test
  fun `when replacement found modifies method visit`() {
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKESPECIAL,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    /* ktlint-disable experimental:argument-list-wrapping */
    fixture
      .getSut(replacements = mapOf(Replacement.FileInputStream.STRING))
      .visitMethodInsn(
        methodVisit.opcode,
        methodVisit.owner,
        methodVisit.name,
        methodVisit.descriptor,
        methodVisit.isInterface,
      )
    /* ktlint-enable experimental:argument-list-wrapping */

    assertEquals(fixture.visitor.methodVisits.size, 2)
    // store original arguments
    assertEquals(fixture.visitor.varVisits[0], VarVisit(Opcodes.ASTORE, 1))
    // load original argument for the original method visit
    assertEquals(fixture.visitor.varVisits[1], VarVisit(Opcodes.ALOAD, 1))
    // original method is visited unchanged
    assertEquals(fixture.visitor.methodVisits[0], methodVisit)

    // load original argument for the replacement/wrapping method visit
    // the target object that we are wrapping will be taken from stack
    assertEquals(fixture.visitor.varVisits[2], VarVisit(Opcodes.ALOAD, 1))
    // replacement/wrapping method visited
    assertEquals(
      fixture.visitor.methodVisits[1],
      MethodVisit(
        Opcodes.INVOKESTATIC,
        "io/sentry/instrumentation/file/SentryFileInputStream${'$'}Factory",
        "create",
        "(Ljava/io/FileInputStream;Ljava/lang/String;)Ljava/io/FileInputStream;",
        isInterface = false,
      ),
    )
  }

  @Test
  fun `when NEW insn with DUP does not modify operand stack`() {
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKESPECIAL,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    val firstPassVisitor =
      MethodNode(Opcodes.ASM7).apply {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitInsn(Opcodes.DUP)
      }

    fixture
      .getSut(
        replacements = mapOf(Replacement.FileInputStream.STRING),
        firstPassVisitor = firstPassVisitor,
      )
      .run {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitMethodInsn(
          methodVisit.opcode,
          methodVisit.owner,
          methodVisit.name,
          methodVisit.descriptor,
          methodVisit.isInterface,
        )
      }

    // DUP was not visited by our visitor
    assertEquals(fixture.visitor.insnVisits.size, 0)
    // ASTORE was not visited by our visitor in the end (count would be 4 otherwise)
    assertEquals(fixture.visitor.varVisits.size, 3)
  }

  @Test
  fun `when NEW insn without DUP - modifies operand stack with DUP and ASTORE`() {
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKESPECIAL,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    val firstPassVisitor =
      MethodNode(Opcodes.ASM7).apply {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitVarInsn(Opcodes.ASTORE, 1)
      }

    fixture
      .getSut(
        replacements = mapOf(Replacement.FileInputStream.STRING),
        firstPassVisitor = firstPassVisitor,
      )
      .run {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitVarInsn(Opcodes.ASTORE, 1)
        visitMethodInsn(
          methodVisit.opcode,
          methodVisit.owner,
          methodVisit.name,
          methodVisit.descriptor,
          methodVisit.isInterface,
        )
      }

    // DUP was visited by our visitor
    assertTrue {
      fixture.visitor.insnVisits.size == 1 &&
        fixture.visitor.insnVisits.first() == InsnVisit(Opcodes.DUP)
    }
    // ASTORE was visited by our visitor in the end
    assertTrue {
      fixture.visitor.varVisits.size == 5 &&
        fixture.visitor.varVisits.last() == VarVisit(Opcodes.ASTORE, 1)
    }
  }

  @Test
  fun `multiple NEW insns`() {
    val firstPassVisitor =
      MethodNode(Opcodes.ASM7).apply {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitVarInsn(Opcodes.ASTORE, 1)

        visitTypeInsn(Opcodes.NEW, "some/random/Class")
        visitInsn(Opcodes.DUP)

        visitTypeInsn(Opcodes.NEW, "java/io/FileOutputStream")
        visitInsn(Opcodes.DUP)
      }

    fixture
      .getSut(
        replacements =
          mapOf(Replacement.FileInputStream.STRING, Replacement.FileOutputStream.STRING),
        firstPassVisitor = firstPassVisitor,
      )
      .run {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitTypeInsn(Opcodes.NEW, "some/random/Class")
        visitTypeInsn(Opcodes.NEW, "java/io/FileOutputStream")
      }
    assertTrue {
      fixture.visitor.insnVisits.size == 1 &&
        fixture.visitor.insnVisits.first() == InsnVisit(Opcodes.DUP)
    }
  }

  @Test
  fun `when NEW insn with DUP followed by ASTORE - modifies operand stack with DUP and ASTORE`() {
    val methodVisit =
      MethodVisit(
        opcode = Opcodes.INVOKESPECIAL,
        owner = "java/io/FileInputStream",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        isInterface = false,
      )
    val firstPassVisitor =
      MethodNode(Opcodes.ASM7).apply {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitInsn(Opcodes.DUP)
        visitVarInsn(Opcodes.ASTORE, 1)
      }

    fixture
      .getSut(
        replacements = mapOf(Replacement.FileInputStream.STRING),
        firstPassVisitor = firstPassVisitor,
      )
      .run {
        visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream")
        visitInsn(Opcodes.DUP)
        visitVarInsn(Opcodes.ASTORE, 1)
        visitMethodInsn(
          methodVisit.opcode,
          methodVisit.owner,
          methodVisit.name,
          methodVisit.descriptor,
          methodVisit.isInterface,
        )
      }

    // DUP was visited by our visitor
    assertTrue {
      fixture.visitor.insnVisits.size == 2 &&
        fixture.visitor.insnVisits.all { it == InsnVisit(Opcodes.DUP) }
    }
    // ASTORE was visited by our visitor in the end
    assertTrue {
      fixture.visitor.varVisits.size == 5 &&
        fixture.visitor.varVisits.last() == VarVisit(Opcodes.ASTORE, 1)
    }
  }
}

data class MethodVisit(
  val opcode: Int,
  val owner: String,
  val name: String,
  val descriptor: String,
  val isInterface: Boolean,
)

data class VarVisit(val opcode: Int, val variable: Int)

data class InsnVisit(val opcode: Int)

class CapturingMethodVisitor : MethodVisitor(Opcodes.ASM7) {

  val methodVisits = mutableListOf<MethodVisit>()
  val varVisits = mutableListOf<VarVisit>()
  val insnVisits = mutableListOf<InsnVisit>()

  override fun visitVarInsn(opcode: Int, variable: Int) {
    super.visitVarInsn(opcode, variable)
    varVisits += VarVisit(opcode, variable)
  }

  override fun visitInsn(opcode: Int) {
    super.visitInsn(opcode)
    insnVisits += InsnVisit(opcode)
  }

  override fun visitMethodInsn(
    opcode: Int,
    owner: String,
    name: String,
    descriptor: String,
    isInterface: Boolean,
  ) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    methodVisits += MethodVisit(opcode, owner, name, descriptor, isInterface)
  }
}
