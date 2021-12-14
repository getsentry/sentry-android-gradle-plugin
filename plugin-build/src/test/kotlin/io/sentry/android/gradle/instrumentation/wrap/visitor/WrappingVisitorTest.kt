package io.sentry.android.gradle.instrumentation.wrap.visitor

import com.android.build.api.instrumentation.ClassData
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import io.sentry.android.gradle.instrumentation.fakes.TestClassData
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import kotlin.test.assertEquals
import org.junit.Test
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class WrappingVisitorTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val visitor = CapturingMethodVisitor()

        fun getSut(
            methodContext: MethodContext,
            classContext: ClassData = TestClassData("io/sentry/RandomClass"),
            replacements: Map<Replacement, Replacement> = mapOf()
        ) = WrappingVisitor(
            Opcodes.ASM9,
            visitor,
            classContext,
            methodContext,
            replacements,
            logger
        )
    }

    private val fixture = Fixture()

    @Test
    fun `invokedynamic is skipped from instrumentation`() {
        val context = MethodContext(Opcodes.ACC_PUBLIC, "test", "()V", null, null)
        val methodVisit = MethodVisit(
            opcode = Opcodes.INVOKEDYNAMIC,
            owner = "java/io/FileInputStream",
            name = "<init>",
            descriptor = "(Ljava/lang/String;)V",
            isInterface = false
        )
        fixture.getSut(context).visitMethodInsn(
            opcode = methodVisit.opcode,
            owner = methodVisit.owner,
            name = methodVisit.name,
            descriptor = methodVisit.descriptor,
            isInterface = methodVisit.isInterface
        )

        assertEquals(
            fixture.logger.capturedMessage,
            "[sentry] INVOKEDYNAMIC skipped from instrumentation for io.sentry.RandomClass.test"
        )
        // method visit should remain unchanged
        assertEquals(fixture.visitor.methodVisits.size, 1)
        assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
    }

    @Test
    fun `when no replacements found does not modify method visit`() {
        val context = MethodContext(Opcodes.ACC_PUBLIC, "test", "()V", null, null)
        val methodVisit = MethodVisit(
            opcode = Opcodes.INVOKEVIRTUAL,
            owner = "java/io/FileInputStream",
            name = "<init>",
            descriptor = "(Ljava/lang/String;)V",
            isInterface = false
        )
        fixture.getSut(context).visitMethodInsn(
            opcode = methodVisit.opcode,
            owner = methodVisit.owner,
            name = methodVisit.name,
            descriptor = methodVisit.descriptor,
            isInterface = methodVisit.isInterface
        )

        // method visit should remain unchanged
        assertEquals(fixture.visitor.methodVisits.size, 1)
        assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
    }

    @Test
    fun `when replacement found and super call in override does not modify method visit`() {
        val context = MethodContext(
            Opcodes.ACC_PUBLIC,
            "openFileInput",
            "(Ljava/lang/String;)Ljava/io/FileInputStream;",
            null,
            null
        )
        val methodVisit = MethodVisit(
            opcode = Opcodes.INVOKESPECIAL,
            owner = "android/content/Context",
            name = "openFileInput",
            descriptor = "(Ljava/lang/String;)Ljava/io/FileInputStream;",
            isInterface = false
        )
        fixture.getSut(
            context,
            classContext = TestClassData("io/sentry/android/sample/LyricsActivity"),
            replacements = mapOf(Replacement.Context.OPEN_FILE_INPUT)
        ).visitMethodInsn(
            opcode = methodVisit.opcode,
            owner = methodVisit.owner,
            name = methodVisit.name,
            descriptor = methodVisit.descriptor,
            isInterface = methodVisit.isInterface
        )

        assertEquals(
            fixture.logger.capturedMessage,
            "[sentry] io.sentry.android.sample.LyricsActivity skipped from instrumentation " +
                "in overridden method openFileInput.(Ljava/lang/String;)Ljava/io/FileInputStream;"
        )
        // method visit should remain unchanged
        assertEquals(fixture.visitor.methodVisits.size, 1)
        assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
    }

    @Test
    fun `when replacement found and super call in constructor does not modify method visit`() {
        val context = MethodContext(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        val methodVisit = MethodVisit(
            opcode = Opcodes.INVOKESPECIAL,
            owner = "java/io/FileInputStream",
            name = "<init>",
            descriptor = "(Ljava/lang/String;)V",
            isInterface = false
        )
        fixture.getSut(
            context,
            classContext = TestClassData(
                "io/sentry/CustomFileInputStream",
                superClasses = listOf("java/io/FileInputStream")
            ),
            replacements = mapOf(Replacement.FileInputStream.STRING)
        ).visitMethodInsn(
            opcode = methodVisit.opcode,
            owner = methodVisit.owner,
            name = methodVisit.name,
            descriptor = methodVisit.descriptor,
            isInterface = methodVisit.isInterface
        )

        assertEquals(
            fixture.logger.capturedMessage,
            "[sentry] io.sentry.CustomFileInputStream skipped from instrumentation in " +
                "constructor <init>.(Ljava/lang/String;)V"
        )
        // method visit should remain unchanged
        assertEquals(fixture.visitor.methodVisits.size, 1)
        assertEquals(fixture.visitor.methodVisits.first(), methodVisit)
    }

    @Test
    fun `when replacement found modifies method visit`() {
        val context = MethodContext(
            Opcodes.ACC_PUBLIC,
            "test",
            "()V",
            null,
            null
        )
        val methodVisit = MethodVisit(
            opcode = Opcodes.INVOKESPECIAL,
            owner = "java/io/FileInputStream",
            name = "<init>",
            descriptor = "(Ljava/lang/String;)V",
            isInterface = false
        )
        /* ktlint-disable experimental:argument-list-wrapping */
        fixture.getSut(context, replacements = mapOf(Replacement.FileInputStream.STRING))
            .visitMethodInsn(
                methodVisit.opcode,
                methodVisit.owner,
                methodVisit.name,
                methodVisit.descriptor,
                methodVisit.isInterface
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
                isInterface = false
            )
        )
    }
}

data class MethodVisit(
    val opcode: Int,
    val owner: String,
    val name: String,
    val descriptor: String,
    val isInterface: Boolean
)

data class VarVisit(
    val opcode: Int,
    val variable: Int
)

class CapturingMethodVisitor : MethodVisitor(Opcodes.ASM9) {

    val methodVisits = mutableListOf<MethodVisit>()
    val varVisits = mutableListOf<VarVisit>()

    override fun visitVarInsn(opcode: Int, variable: Int) {
        super.visitVarInsn(opcode, variable)
        varVisits += VarVisit(opcode, variable)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        methodVisits += MethodVisit(opcode, owner, name, descriptor, isInterface)
    }
}
