@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import io.sentry.android.gradle.instrumentation.ChainedInstrumentable
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class AndroidXSQLiteDriverTest {

  @get:Rule val tmpDir = TemporaryFolder()

  private val instrumentable = AndroidXSQLiteDriver()

  @Test
  fun `isInstrumentable returns true for RoomDatabase Builder classes`() {
    assertTrue(
      instrumentable.isInstrumentable(TestClassContext("androidx.room.RoomDatabase\$Builder"))
    )
    assertTrue(
      instrumentable.isInstrumentable(TestClassContext("androidx.room3.RoomDatabase\$Builder"))
    )
  }

  @Test
  fun `isInstrumentable returns false for unrelated classes`() {
    assertFalse(instrumentable.isInstrumentable(TestClassContext("com.example.RoomConfig")))
    assertFalse(instrumentable.isInstrumentable(TestClassContext("io.sentry.Sentry")))
    assertFalse(instrumentable.isInstrumentable(TestClassContext("com.example.FakeSetDriver")))
  }

  @Test
  fun `ChainedInstrumentable does not instrument unrelated classes`() {
    val className = "com.example.NoSetDriver"
    val originalBytes = loadNoSetDriverFixtureBytes()
    val instrumentedBytes = instrumentThroughChain(className, originalBytes)

    assertEquals(0, SQLiteDriverBytecodeTestUtil.countWrapCalls(instrumentedBytes))
  }

  @Test
  fun `does not wrap when a visited class has no setDriver method`() {
    // The Room < 2.7 production path: RoomDatabase$Builder matches the allowlist (so getVisitor
    // runs), but the class has no setDriver(SQLiteDriver) and must pass through without a wrap.
    val instrumentedBytes =
      instrumentDirectly("com.example.NoSetDriver", loadNoSetDriverFixtureBytes())

    assertEquals(0, SQLiteDriverBytecodeTestUtil.countWrapCalls(instrumentedBytes))
  }

  private fun instrumentThroughChain(className: String, bytes: ByteArray): ByteArray =
    instrument(ChainedInstrumentable(listOf(instrumentable)), className, bytes)

  private fun instrumentDirectly(className: String, bytes: ByteArray): ByteArray =
    instrument(instrumentable, className, bytes)

  private fun instrument(
    classInstrumentable: ClassInstrumentable,
    className: String,
    bytes: ByteArray,
  ): ByteArray {
    val classReader = ClassReader(bytes)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val classVisitor =
      classInstrumentable.getVisitor(
        TestClassContext(className),
        Opcodes.ASM9,
        classWriter,
        parameters = TestSpanAddingParameters(inMemoryDir = tmpDir.root),
      )
    classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)
    return classWriter.toByteArray()
  }

  /**
   * `NoSetDriver.class`: hand-compiled `public class NoSetDriver { int unrelated(int) }`. Shape is
   * irrelevant / any class without `setDriver(SQLiteDriver)` works.
   */
  private fun loadNoSetDriverFixtureBytes(): ByteArray =
    FileInputStream(
        "src/test/resources/testFixtures/instrumentation/androidxSqliteDriver/NoSetDriver.class"
      )
      .use { it.readBytes() }
}
