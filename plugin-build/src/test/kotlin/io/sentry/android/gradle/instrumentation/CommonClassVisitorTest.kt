package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import io.sentry.android.gradle.instrumentation.util.CatchingMethodVisitor
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class CommonClassVisitorTest {

    class Fixture {

        fun getSut(tmpDir: File, debug: Boolean = false) =
            CommonClassVisitor(
                Opcodes.ASM7,
                ParentClassVisitor(),
                "SomeClass",
                listOf(TestInstrumentable()),
                TestSpanAddingParameters(debugOutput = debug, inMemoryDir = tmpDir)
            )
    }

    @get:Rule
    val tmpDir = TemporaryFolder()

    private val fixture = Fixture()

    @Test
    fun `when debug - creates a file with class name on init`() {
        fixture.getSut(tmpDir.root, true)

        val file = File(tmpDir.root, "SomeClass-instrumentation.log")
        assertTrue { file.exists() }
    }

    @Test
    fun `when debug and is instrumentable - prepends with TraceMethodVisitor`() {
        val mv = fixture.getSut(tmpDir.root, true)
            .visitMethod(Opcodes.ACC_PUBLIC, "test", null, null, null)

        mv.visitVarInsn(Opcodes.ASTORE, 0)
        mv.visitEnd()

        // we read the file and compare its content to ensure that TraceMethodVisitor was called and
        // wrote the instructions into the file
        val file = File(tmpDir.root, "SomeClass-instrumentation.log")
        assertEquals(
            file.readText(),
            """
            |function test null
            |    ASTORE 0
            |
            |
            """.trimMargin()
        )
    }

    @Test
    fun `when no debug and is instrumentable - skips TraceMethodVisitor`() {
        val mv = fixture.getSut(tmpDir.root, true)
            .visitMethod(Opcodes.ACC_PUBLIC, "other", null, null, null)

        mv.visitVarInsn(Opcodes.ASTORE, 0)
        mv.visitEnd()

        // we read the file and compare its content to ensure that TraceMethodVisitor was skipped
        val file = File(tmpDir.root, "SomeClass-instrumentation.log")
        assertTrue { file.readText().isEmpty() }
    }

    @Test
    fun `when matches method name returns instrumentable visitor wrapped into catching visitor`() {
        val mv =
            fixture.getSut(tmpDir.root).visitMethod(Opcodes.ACC_PUBLIC, "test", null, null, null)

        assertTrue { mv is CatchingMethodVisitor }
    }

    @Test
    fun `when doesn't match method name return original visitor`() {
        val mv =
            fixture.getSut(tmpDir.root).visitMethod(Opcodes.ACC_PUBLIC, "other", null, null, null)

        assertTrue { mv is ParentClassVisitor.ParentMethodVisitor }
    }
}

class ParentClassVisitor : ClassVisitor(Opcodes.ASM7) {

    inner class ParentMethodVisitor : MethodVisitor(Opcodes.ASM7)

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor = ParentMethodVisitor()
}

class TestInstrumentable : MethodInstrumentable {

    inner class TestVisitor(originalVisitor: MethodVisitor) :
        MethodVisitor(Opcodes.ASM7, originalVisitor)

    override val fqName: String get() = "test"

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = TestVisitor(originalVisitor)
}
