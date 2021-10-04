package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import java.io.FileInputStream
import java.io.PrintWriter
import java.io.StringWriter
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter

@Suppress("UnstableApiUsage")
@RunWith(Parameterized::class)
class VisitorTest(
    private val instrumentedProject: String,
    private val className: String,
    private val instrumentable: Instrumentable<ClassVisitor, ClassContext>,
    private val classContext: ClassContext?
) {

    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun `instrumented class passes Java verifier`() {
        // first we read the original bytecode and pass it through the ClassWriter, so it computes
        // MAXS for us automatically (that's what AGP will do as well)
        val inputStream =
            FileInputStream(
                "src/test/resources/testFixtures/instrumentation/$instrumentedProject/$className.class"
            )
        val classReader = ClassReader(inputStream)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val classContext = this.classContext ?: TestClassContext(instrumentable.fqName)
        val classVisitor = instrumentable.getVisitor(
            classContext,
            Opcodes.ASM7,
            classWriter,
            parameters = TestSpanAddingParameters(tmpDir.root)
        )
        // here we visit the bytecode, so it gets modified by our instrumentation visitor
        classReader.accept(classVisitor, ClassReader.SKIP_DEBUG or ClassReader.EXPAND_FRAMES)

        // after that we convert the modified bytecode with computed MAXS back to byte array
        // and pass it through CheckClassAdapter to verify that the bytecode is correct and can be accepted by JVM
        val bytes = classWriter.toByteArray()
        val verifyReader = ClassReader(bytes)
        val checkAdapter = CheckClassAdapter(ClassWriter(0), true)
        verifyReader.accept(checkAdapter, 0)

        // this will verify that the output of the above verifyReader is empty
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        CheckClassAdapter.verify(
            verifyReader,
            false,
            printWriter
        )
        assertEquals(stringWriter.toString(), "")
    }

    @After
    fun printLogs() {
        tmpDir.root.listFiles()
            ?.filter { it.name.contains("instrumentation") }
            ?.forEach {
                print(it.readText())
            }
    }

    companion object {

        @Parameterized.Parameters(name = "{0}/{1}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf("androidxSqlite", "FrameworkSQLiteDatabase", AndroidXSQLiteDatabase(), null),
            arrayOf("androidxSqlite", "FrameworkSQLiteStatement", AndroidXSQLiteStatement(), null),
            roomDaoTestParameters("DeleteAndReturnUnit"),
            roomDaoTestParameters("InsertAndReturnLong"),
            roomDaoTestParameters("InsertAndReturnUnit"),
            roomDaoTestParameters("UpdateAndReturnUnit"),
            deletionDaoTestParameters("DeleteAndReturnInteger"),
            deletionDaoTestParameters("DeleteAndReturnVoid"),
            deletionDaoTestParameters("DeleteQuery"),
            deletionDaoTestParameters("DeleteQueryAndReturnInteger")
        )

        private fun roomDaoTestParameters(suffix: String = "") = arrayOf(
            "androidxRoom",
            "TracksDao_$suffix",
            AndroidXRoomDao(),
            TestClassContext("TracksDao_$suffix") { lookupName ->
                TestClassData(lookupName, classAnnotations = listOf(AndroidXRoomDao().fqName))
            }
        )

        private fun deletionDaoTestParameters(suffix: String = "") = arrayOf(
            "androidxRoom/delete",
            "DeletionDao_$suffix",
            AndroidXRoomDao(),
            TestClassContext("DeletionDao_$suffix") { lookupName ->
                TestClassData(lookupName, classAnnotations = listOf(AndroidXRoomDao().fqName))
            }
        )
    }
}
