package io.sentry.android.gradle.instrumentation.webview

import com.android.build.api.instrumentation.ClassData
import com.google.common.truth.Truth.assertThat
import io.sentry.android.gradle.instrumentation.classloader.GeneratingMissingClassesClassLoader
import io.sentry.android.gradle.instrumentation.fakes.TestClassContext
import io.sentry.android.gradle.instrumentation.fakes.TestClassData
import io.sentry.android.gradle.instrumentation.fakes.TestSpanAddingParameters
import java.io.PrintWriter
import java.io.StringWriter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.util.CheckClassAdapter

class WebViewLoadUrlTest {

  @get:Rule val tmpDir = TemporaryFolder()

  private data class Call(val opcode: Int, val owner: String, val name: String, val desc: String)

  @Test
  fun `replaces loadUrl on a WebView`() {
    assertThat(instrument(Call(Opcodes.INVOKEVIRTUAL, WEB_VIEW, "loadUrl", LOAD_URL)).single())
      .isEqualTo(Call(Opcodes.INVOKESTATIC, ADAPTER, "loadUrl", "(L$WEB_VIEW;Ljava/lang/String;)V"))
  }

  @Test
  fun `replaces loadUrl with headers on a WebView`() {
    assertThat(
        instrument(Call(Opcodes.INVOKEVIRTUAL, WEB_VIEW, "loadUrl", LOAD_URL_HEADERS)).single()
      )
      .isEqualTo(
        Call(
          Opcodes.INVOKESTATIC,
          ADAPTER,
          "loadUrl",
          "(L$WEB_VIEW;Ljava/lang/String;Ljava/util/Map;)V",
        )
      )
  }

  @Test
  fun `replaced call passes the verifier`() {
    val bytes = instrumentToBytes(Call(Opcodes.INVOKEVIRTUAL, WEB_VIEW, "loadUrl", LOAD_URL))

    val output = StringWriter()
    CheckClassAdapter.verify(
      ClassReader(bytes),
      GeneratingMissingClassesClassLoader(),
      false,
      PrintWriter(output),
    )

    assertThat(output.toString()).isEmpty()
  }

  @Test
  fun `replaces loadUrl on a WebView subclass`() {
    val call = Call(Opcodes.INVOKEVIRTUAL, MY_WEB_VIEW, "loadUrl", LOAD_URL)

    assertThat(instrument(call, webViewSubclass).single().owner).isEqualTo(ADAPTER)
  }

  @Test
  fun `keeps super calls from a WebView subclass`() {
    val call = Call(Opcodes.INVOKESPECIAL, WEB_VIEW, "loadUrl", LOAD_URL)

    assertThat(instrument(call).single()).isEqualTo(call)
  }

  @Test
  fun `keeps loadUrl on unrelated classes`() {
    val call = Call(Opcodes.INVOKEVIRTUAL, "com/example/Browser", "loadUrl", LOAD_URL)

    assertThat(instrument(call, unrelatedClass).single()).isEqualTo(call)
  }

  @Test
  fun `keeps loadUrl on classes that cannot be resolved`() {
    val call = Call(Opcodes.INVOKEVIRTUAL, MY_WEB_VIEW, "loadUrl", LOAD_URL)

    assertThat(instrument(call).single()).isEqualTo(call)
  }

  @Test
  fun `keeps other WebView methods`() {
    val call = Call(Opcodes.INVOKEVIRTUAL, WEB_VIEW, "evaluateJavascript", LOAD_URL)

    assertThat(instrument(call).single()).isEqualTo(call)
  }

  @Test
  fun `keeps other WebView overloads`() {
    val call = Call(Opcodes.INVOKEVIRTUAL, WEB_VIEW, "loadUrl", "(Ljava/lang/Object;)V")

    assertThat(instrument(call).single()).isEqualTo(call)
  }

  @Test
  fun `does not instrument Sentry classes`() {
    assertThat(WebViewLoadUrl().isInstrumentable(TestClassContext("io.sentry.android.core.Foo")))
      .isFalse()
    assertThat(WebViewLoadUrl().isInstrumentable(TestClassContext("com.example.Foo"))).isTrue()
  }

  private val webViewSubclass: (String) -> ClassData? = { name ->
    TestClassData(name, superClasses = listOf("android.webkit.WebView", "java.lang.Object"))
  }

  private val unrelatedClass: (String) -> ClassData? = { name ->
    TestClassData(name, superClasses = listOf("java.lang.Object"))
  }

  private fun instrument(call: Call, classLoader: (String) -> ClassData? = { null }): List<Call> {
    val node = ClassNode()
    ClassReader(instrumentToBytes(call, classLoader)).accept(node, 0)
    return node.methods
      .single { it.name == "call" }
      .instructions
      .filterIsInstance<MethodInsnNode>()
      .map { Call(it.opcode, it.owner, it.name, it.desc) }
  }

  private fun instrumentToBytes(
    call: Call,
    classLoader: (String) -> ClassData? = { null },
  ): ByteArray {
    val reader = ClassReader(callerClass(call))
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    val visitor =
      WebViewLoadUrl()
        .getVisitor(
          TestClassContext("com.example.Caller", classLoader),
          Opcodes.ASM9,
          writer,
          TestSpanAddingParameters(inMemoryDir = tmpDir.root),
        )
    reader.accept(visitor, ClassReader.SKIP_FRAMES)
    return writer.toByteArray()
  }

  /** A class whose only method passes its arguments to [call]. */
  private fun callerClass(call: Call): ByteArray {
    val arguments = call.desc.substring(1, call.desc.indexOf(')'))
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
    writer.visit(
      Opcodes.V1_8,
      Opcodes.ACC_PUBLIC,
      "com/example/Caller",
      null,
      "java/lang/Object",
      null,
    )
    writer
      .visitMethod(
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
        "call",
        "(L${call.owner};$arguments)V",
        null,
        null,
      )
      .apply {
        visitCode()
        repeat(arguments.count { it == ';' } + 1) { visitVarInsn(Opcodes.ALOAD, it) }
        visitMethodInsn(call.opcode, call.owner, call.name, call.desc, false)
        visitInsn(Opcodes.RETURN)
        visitMaxs(0, 0)
        visitEnd()
      }
    writer.visitEnd()
    return writer.toByteArray()
  }

  private companion object {
    const val ADAPTER = "io/sentry/android/core/SentryWebViewTracingAdapter"
    const val WEB_VIEW = "android/webkit/WebView"
    const val MY_WEB_VIEW = "com/example/MyWebView"
    const val LOAD_URL = "(Ljava/lang/String;)V"
    const val LOAD_URL_HEADERS = "(Ljava/lang/String;Ljava/util/Map;)V"
  }
}
