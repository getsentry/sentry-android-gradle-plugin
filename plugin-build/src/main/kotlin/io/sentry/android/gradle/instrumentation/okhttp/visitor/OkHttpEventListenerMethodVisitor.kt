package io.sentry.android.gradle.instrumentation.okhttp.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.util.SemVer
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class OkHttpEventListenerMethodVisitor(
  apiVersion: Int,
  originalVisitor: MethodVisitor,
  instrumentableContext: MethodContext,
  private val okHttpVersion: SemVer,
  private val useSentryAndroidOkHttp: Boolean,
) :
  AdviceAdapter(
    apiVersion,
    originalVisitor,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor,
  ) {

  private val sentryOkHttpEventListener =
    if (useSentryAndroidOkHttp) {
      "io/sentry/android/okhttp/SentryOkHttpEventListener"
    } else {
      "io/sentry/okhttp/SentryOkHttpEventListener"
    }

  override fun onMethodEnter() {
    super.onMethodEnter()
    // Add the following call at the beginning of the constructor with the Builder parameter:
    // builder.eventListener(new SentryOkHttpEventListener(builder.eventListenerFactory));

    // OkHttpClient.Builder is the parameter, retrieved here
    visitVarInsn(Opcodes.ALOAD, 1)

    // Let's declare the SentryOkHttpEventListener variable
    visitTypeInsn(Opcodes.NEW, sentryOkHttpEventListener)

    // The SentryOkHttpEventListener constructor, which is called later, will consume the
    //  element without pushing anything back to the stack (<init> returns void).
    // Dup will give a reference to the SentryOkHttpEventListener after the constructor call
    visitInsn(Opcodes.DUP)

    // Puts parameter OkHttpClient.Builder on top of the stack.
    visitVarInsn(Opcodes.ALOAD, 1)

    // Read the "eventListenerFactory" field from OkHttpClient.Builder
    // Implementation changed in v4 (including 4.0.0-RCx)
    if (okHttpVersion.major >= 4) {
      // Call the getter
      visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "okhttp3/OkHttpClient\$Builder",
        "getEventListenerFactory\$okhttp",
        "()Lokhttp3/EventListener\$Factory;",
        false,
      )
    } else {
      // Read the field
      visitFieldInsn(
        Opcodes.GETFIELD,
        "okhttp3/OkHttpClient\$Builder",
        "eventListenerFactory",
        "Lokhttp3/EventListener\$Factory;",
      )
    }

    // Call SentryOkHttpEventListener constructor passing "eventListenerFactory" as parameter
    visitMethodInsn(
      Opcodes.INVOKESPECIAL,
      sentryOkHttpEventListener,
      "<init>",
      "(Lokhttp3/EventListener\$Factory;)V",
      false,
    )

    // Call "eventListener" function of OkHttpClient.Builder passing SentryOkHttpEventListener
    visitMethodInsn(
      Opcodes.INVOKEVIRTUAL,
      "okhttp3/OkHttpClient\$Builder",
      "eventListener",
      "(Lokhttp3/EventListener;)Lokhttp3/OkHttpClient\$Builder;",
      false,
    )
  }
}
