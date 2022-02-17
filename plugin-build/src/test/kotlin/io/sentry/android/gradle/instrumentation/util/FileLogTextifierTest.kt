package io.sentry.android.gradle.instrumentation.util

import java.io.File
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

class FileLogTextifierTest {

    class Fixture {

        fun getSut(tmpFile: File) =
            FileLogTextifier(
                Opcodes.ASM9,
                tmpFile,
                "SomeMethod",
                "(Ljava/lang/Throwable;)V"
            )

        fun visitMethodInstructions(sut: FileLogTextifier) {
            sut.visitVarInsn(Opcodes.ASTORE, 0)
            sut.visitLabel(Label())
            sut.visitLdcInsn("db")
        }
    }

    @get:Rule
    val tmpDir = TemporaryFolder()

    private val fixture = Fixture()

    @Test
    fun `prints methodName on ccreation`() {
        fixture.getSut(tmpDir.newFile("instrumentation.log"))

        val file = File(tmpDir.root, "instrumentation.log")
        assertEquals(
            file.readText(),
            "function SomeMethod (Ljava/lang/Throwable;)V\n"
        )
    }

    @Test
    fun `visitMethodEnd flushes output to file if hasn't thrown`() {
        val sut = fixture.getSut(tmpDir.newFile("instrumentation.log"))
        fixture.visitMethodInstructions(sut)
        sut.visitMethodEnd()

        val file = File(tmpDir.root, "instrumentation.log")
        assertEquals(
            file.readText(),
            """
            |function SomeMethod (Ljava/lang/Throwable;)V
            |    ASTORE 0
            |   L0
            |    LDC "db"
            |
            |
            """.trimMargin()
        )
    }

    @Test
    fun `visitMethodEnd does nothing if has thrown`() {
        val sut = fixture.getSut(tmpDir.newFile("instrumentation.log"))
        sut.handle(RuntimeException())
        fixture.visitMethodInstructions(sut)
        sut.visitMethodEnd()

        val file = File(tmpDir.root, "instrumentation.log")
        // sut.handle will add one more newline to the end of file, but actual visited instructions
        // will not be flushed to file
        assertEquals(
            file.readText(),
            """
            |function SomeMethod (Ljava/lang/Throwable;)V
            |
            |
            """.trimMargin()
        )
    }

    @Test
    fun `handle exception flushes output to file`() {
        val sut = fixture.getSut(tmpDir.newFile("instrumentation.log"))
        fixture.visitMethodInstructions(sut)
        sut.handle(RuntimeException())

        val file = File(tmpDir.root, "instrumentation.log")
        assertEquals(
            file.readText(),
            """
            |function SomeMethod (Ljava/lang/Throwable;)V
            |    ASTORE 0
            |   L0
            |    LDC "db"
            |
            |
            """.trimMargin()
        )
    }
}
