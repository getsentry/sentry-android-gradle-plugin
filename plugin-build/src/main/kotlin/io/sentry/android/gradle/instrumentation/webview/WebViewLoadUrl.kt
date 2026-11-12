package io.sentry.android.gradle.instrumentation.webview

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Replaces `WebView.loadUrl` calls with calls to `SentryWebViewTracingAdapter`, which refreshes the
 * trace propagated to the page and then calls the original `loadUrl`.
 */
class WebViewLoadUrl : ClassInstrumentable {

  companion object {
    private const val VISITOR_NAME = "WebViewLoadUrl"
  }

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor =
    CommonClassVisitor(
      apiVersion,
      originalVisitor,
      VISITOR_NAME,
      listOf(WebViewLoadUrlMethodInstrumentable(instrumentableContext)),
      parameters,
    )

  override fun isInstrumentable(data: ClassContext) = !data.isSentryClass()
}

class WebViewLoadUrlMethodInstrumentable(private val classContext: ClassContext) :
  MethodInstrumentable {

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor = WebViewLoadUrlMethodVisitor(apiVersion, originalVisitor, classContext)

  override fun isInstrumentable(data: MethodContext): Boolean = true
}

class WebViewLoadUrlMethodVisitor(
  apiVersion: Int,
  originalVisitor: MethodVisitor,
  private val classContext: ClassContext,
) : MethodVisitor(apiVersion, originalVisitor) {

  override fun visitMethodInsn(
    opcode: Int,
    owner: String,
    name: String,
    descriptor: String,
    isInterface: Boolean,
  ) {
    if (shouldReplace(opcode, owner, name, descriptor)) {
      // The receiver already on the stack becomes the adapter's first argument.
      mv.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        ADAPTER,
        name,
        "(L$WEB_VIEW;${descriptor.substring(1)}",
        false,
      )
    } else {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
  }

  private fun shouldReplace(opcode: Int, owner: String, name: String, descriptor: String): Boolean =
    // Only virtual calls: a subclass's super.loadUrl() is INVOKESPECIAL, and rewriting it would
    // make the adapter call the subclass's override again, forever.
    opcode == Opcodes.INVOKEVIRTUAL &&
      name == LOAD_URL &&
      descriptor in LOAD_URL_DESCRIPTORS &&
      isWebView(owner)

  private fun isWebView(owner: String): Boolean =
    owner == WEB_VIEW ||
      classContext.loadClassData(owner.replace('/', '.'))?.superClasses?.contains(WEB_VIEW_FQN) ==
        true

  private companion object {
    const val ADAPTER = "io/sentry/android/core/SentryWebViewTracingAdapter"
    const val WEB_VIEW = "android/webkit/WebView"
    const val WEB_VIEW_FQN = "android.webkit.WebView"
    const val LOAD_URL = "loadUrl"
    val LOAD_URL_DESCRIPTORS =
      setOf("(Ljava/lang/String;)V", "(Ljava/lang/String;Ljava/util/Map;)V")
  }
}
