@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor

import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.AndroidXSQLiteDriver
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Asserts the bytecode-level behavior of [AndroidXSQLiteDriver] / [SQLiteDriverCallSiteVisitor] on
 * each fixture. The sibling [io.sentry.android.gradle.instrumentation.VisitorTest] runs all
 * fixtures through [org.objectweb.asm.util.CheckClassAdapter] to confirm bytecode validity; this
 * test verifies that the visitor actually performs the wrap (or correctly skips when there is no
 * `setDriver` call site).
 *
 * Per the PRD Testing Decisions section, each positive fixture must end up with exactly one
 * `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create` immediately before each `INVOKEVIRTUAL
 * setDriver`. The no-op case (`NoSetDriver`) must pass through unchanged.
 */
class SQLiteDriverCallSiteVisitorTest {

  @get:Rule val tmpDir = TemporaryFolder()

  @Test
  fun `wraps inline construction setDriver call site`() {
    assertWrapsExactly("InlineConstruction", expectedWrapCount = 1)
  }

  @Test
  fun `wraps setDriver call site when local is typed as concrete impl`() {
    assertWrapsExactly("LocalTypedAsImpl", expectedWrapCount = 1)
  }

  @Test
  fun `wraps setDriver call site when local is erased to SQLiteDriver interface holding a bridge`() {
    assertWrapsExactly("LocalTypedAsBridge", expectedWrapCount = 1)
  }

  @Test
  fun `wraps setDriver call site for the SupportSQLiteDriver bridge case`() {
    // Bridge-skip happens inside SentrySQLiteDriver.create() in the SDK, not in the visitor.
    // The visitor unconditionally inserts the wrap; the SDK handles the no-op.
    assertWrapsExactly("InlineBridge", expectedWrapCount = 1)
  }

  @Test
  fun `wraps setDriver call site when argument is loaded from a field`() {
    assertWrapsExactly("FieldLoad", expectedWrapCount = 1)
  }

  @Test
  fun `wraps setDriver call site when argument comes from a factory method`() {
    assertWrapsExactly("FactoryReturn", expectedWrapCount = 1)
  }

  @Test
  fun `wraps each setDriver call site when a method contains multiple invocations`() {
    assertWrapsExactly("TwoSetDriver", expectedWrapCount = 2)
  }

  @Test
  fun `wraps setDriver call site invoked through an interface receiver`() {
    assertWrapsExactly("InvokeInterface", expectedWrapCount = 1, expectInvokeInterface = true)
  }

  @Test
  fun `wraps setDriver call site when a checkcast precedes the invocation`() {
    // The argument is narrowed to SQLiteDriver by a checkcast immediately before setDriver (the
    // shape kotlinc emits for an inferred-type local). The wrap must still land between the
    // checkcast and the call.
    assertWrapsExactly("InferredLocal", expectedWrapCount = 1)
  }

  @Test
  fun `wraps an already-wrapped setDriver call site (idempotency is owned by the SDK)`() {
    // The plugin cannot inspect runtime types; it wraps unconditionally. Idempotency for
    // already-wrapped drivers is enforced by SentrySQLiteDriver.create() in the SDK, so the
    // post-instrumentation bytecode contains two calls to create() for this fixture.
    assertWrapsExactly("ManualWrap", expectedWrapCount = 2)
  }

  @Test
  fun `does not insert a wrap on classes without a setDriver call site`() {
    assertEquals(
      0,
      countWrapCalls(instrument("NoSetDriver")),
      "Visitor inserted a wrap on a fixture with no setDriver call site",
    )
  }

  private fun assertWrapsExactly(
    fixtureName: String,
    expectedWrapCount: Int,
    expectInvokeInterface: Boolean = false,
  ) {
    val instrumentedBytes = instrument(fixtureName)
    val classNode = ClassNode().also { ClassReader(instrumentedBytes).accept(it, 0) }

    var wrapCount = 0
    var setDriverCount = 0
    var invokeInterfaceSetDriverCount = 0
    var wrapsImmediatelyBeforeSetDriver = 0
    for (method in classNode.methods) {
      var prev: AbstractInsnNode? = null
      var insn: AbstractInsnNode? = method.instructions.first
      while (insn != null) {
        if (insn is MethodInsnNode) {
          if (isWrapCall(insn)) wrapCount++
          if (isSetDriverCall(insn)) {
            setDriverCount++
            if (insn.opcode == Opcodes.INVOKEINTERFACE) invokeInterfaceSetDriverCount++
            val prevMeaningful = prev?.previousRealInsn()
            if (prevMeaningful is MethodInsnNode && isWrapCall(prevMeaningful)) {
              wrapsImmediatelyBeforeSetDriver++
            }
          }
        }
        prev = insn
        insn = insn.next
      }
    }

    assertEquals(
      expectedWrapCount,
      wrapCount,
      "Expected $expectedWrapCount INVOKESTATIC SentrySQLiteDriver.create call(s) in $fixtureName, found $wrapCount",
    )
    assertTrue(setDriverCount > 0, "$fixtureName has no setDriver call after instrumentation")
    assertEquals(
      setDriverCount,
      wrapsImmediatelyBeforeSetDriver,
      "Each setDriver call in $fixtureName must be immediately preceded by SentrySQLiteDriver.create()",
    )
    if (expectInvokeInterface) {
      assertTrue(
        invokeInterfaceSetDriverCount > 0,
        "$fixtureName should contain at least one INVOKEINTERFACE setDriver call",
      )
    }
  }

  private fun instrument(fixtureName: String): ByteArray {
    val bytes = readFixtureBytes(fixtureName)
    val classReader = ClassReader(bytes)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val instrumentable = AndroidXSQLiteDriver()
    val classVisitor =
      instrumentable.getVisitor(
        TestClassContext("com.example.$fixtureName"),
        Opcodes.ASM9,
        classWriter,
        parameters = TestSpanAddingParameters(inMemoryDir = tmpDir.root),
      )
    classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)
    return classWriter.toByteArray()
  }

  private fun readFixtureBytes(fixtureName: String): ByteArray =
    FileInputStream(
        "src/test/resources/testFixtures/instrumentation/androidxSqliteDriver/$fixtureName.class"
      )
      .use { it.readBytes() }

  /**
   * Walks backward from `this`, skipping label/line-number/frame pseudo-nodes (opcode < 0), until
   * finding the nearest real bytecode instruction at-or-before this node. Returns `null` if no real
   * instruction is found before reaching the head of the list.
   */
  private fun AbstractInsnNode.previousRealInsn(): AbstractInsnNode? {
    var node: AbstractInsnNode? = this
    while (node != null && node.opcode < 0) {
      node = node.previous
    }
    return node
  }

  private fun countWrapCalls(bytes: ByteArray): Int {
    val classNode = ClassNode().also { ClassReader(bytes).accept(it, 0) }
    return classNode.methods.sumOf { method ->
      var count = 0
      var insn: AbstractInsnNode? = method.instructions.first
      while (insn != null) {
        if (insn is MethodInsnNode && isWrapCall(insn)) count++
        insn = insn.next
      }
      count
    }
  }

  private fun isWrapCall(insn: MethodInsnNode): Boolean =
    insn.opcode == Opcodes.INVOKESTATIC &&
      insn.owner == SENTRY_SQLITE_DRIVER &&
      insn.name == CREATE &&
      insn.desc == SENTRY_CREATE_DESCRIPTOR

  private fun isSetDriverCall(insn: MethodInsnNode): Boolean =
    insn.name == SET_DRIVER && insn.desc.startsWith(SET_DRIVER_DESCRIPTOR_PREFIX)

  companion object {
    private const val SENTRY_SQLITE_DRIVER = "io/sentry/sqlite/SentrySQLiteDriver"
    private const val CREATE = "create"
    private const val SENTRY_CREATE_DESCRIPTOR =
      "(Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;"
    private const val SET_DRIVER = "setDriver"
    private const val SET_DRIVER_DESCRIPTOR_PREFIX = "(Landroidx/sqlite/SQLiteDriver;)"
  }
}
