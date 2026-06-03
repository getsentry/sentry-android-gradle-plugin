package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.androidx.sqlite.AndroidXSQLiteDriver
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.FileInputStream
import junit.framework.TestCase.assertEquals
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

/**
 * Asserts the number of injected `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create` calls
 * per `setDriver` call-site fixture matches the expected WRAP (1) / SKIP (0) decision. This is the
 * CI regression fence for the two failure modes: wrap->skip (missed span) and skip->wrap (double
 * span, the no-double-wrap guarantee). The verifier-passes check for these same fixtures lives in
 * [VisitorTest].
 *
 * "Injected" = count in the instrumented output minus count in the original, so the
 * already-Sentry-wrapped fixture (which already contains one manual `create`) correctly reports 0
 * injected.
 */
@Suppress("UnstableApiUsage")
@RunWith(Parameterized::class)
class SQLiteDriverVisitorTest(
  private val className: String,
  private val expectedInjectedCreates: Int,
) {

  @get:Rule val tmpDir = TemporaryFolder()

  @Test
  fun `injects expected number of SentrySQLiteDriver_create calls`() {
    val path = "src/test/resources/testFixtures/instrumentation/androidxSqlite/$className.class"

    // A ClassReader is immutable and re-acceptable, so one read of the file serves both the
    // baseline count and the instrumentation pass.
    val classReader = FileInputStream(path).use { ClassReader(it) }
    val originalCreates = countSentryCreateCalls(classReader)

    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val instrumentable = AndroidXSQLiteDriver()
    val classVisitor: ClassVisitor =
      instrumentable.getVisitor(
        TestClassContext(className),
        Opcodes.ASM9,
        classWriter,
        parameters = TestSpanAddingParameters(inMemoryDir = tmpDir.root),
      )
    classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)

    val instrumentedCreates = countSentryCreateCalls(ClassReader(classWriter.toByteArray()))

    assertEquals(
      "Unexpected injected SentrySQLiteDriver.create count for $className",
      expectedInjectedCreates,
      instrumentedCreates - originalCreates,
    )
  }

  private fun countSentryCreateCalls(reader: ClassReader): Int {
    var count = 0
    reader.accept(
      object : ClassVisitor(Opcodes.ASM9) {
        override fun visitMethod(
          access: Int,
          name: String?,
          descriptor: String?,
          signature: String?,
          exceptions: Array<out String>?,
        ): MethodVisitor {
          return object : MethodVisitor(Opcodes.ASM9) {
            override fun visitMethodInsn(
              opcode: Int,
              owner: String?,
              name: String?,
              descriptor: String?,
              isInterface: Boolean,
            ) {
              if (
                opcode == Opcodes.INVOKESTATIC &&
                  owner == "io/sentry/sqlite/SentrySQLiteDriver" &&
                  name == "create" &&
                  descriptor == "(Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;"
              ) {
                count++
              }
            }
          }
        }
      },
      ClassReader.SKIP_FRAMES,
    )
    return count
  }

  companion object {

    @Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun parameters() =
      listOf(
        // WRAP cases
        arrayOf("SetDriverConcrete", 1),
        arrayOf("SetDriverConcreteLocal", 1),
        // SKIP cases
        arrayOf("SetDriverBridge", 0), // the hard no-double-wrap guarantee
        arrayOf("SetDriverSentryTyped", 0), // arg statically typed as SentrySQLiteDriver
        arrayOf("SetDriverAlreadySentry", 0), // create() return -> erased to SQLiteDriver iface
        arrayOf("SetDriverBareInterface", 0),
      )
  }
}
