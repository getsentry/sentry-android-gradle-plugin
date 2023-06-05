package io.sentry.android.gradle.instrumentation.okhttp.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class OkHttpEventListenerMethodVisitor(
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    instrumentableContext: MethodContext
) : AdviceAdapter(
    apiVersion,
    originalVisitor,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor
) {

    override fun onMethodEnter() {
        super.onMethodEnter()
        // Add the following call at the beginning of the constructor with the Builder parameter:
        // builder.eventListener(new SentryOkHttpEventListener(builder.eventListenerFactory));

        // OkHttpClient.Builder is the parameter, retrieved here
        visitVarInsn(Opcodes.ALOAD, 1)

        // Let's declare the SentryOkHttpEventListener variable
        visitTypeInsn(Opcodes.NEW, "io/sentry/android/okhttp/SentryOkHttpEventListener")

        // Than we just copy it - I don't really know why it's needed, i just know it's needed
        visitInsn(Opcodes.DUP)

        // Puts parameter OkHttpClient.Builder on top of the stack.
        visitVarInsn(Opcodes.ALOAD, 1)

        // Read the "eventListenerFactory" field from OkHttpClient.Builder
        visitFieldInsn(
            Opcodes.GETFIELD,
            "okhttp3/OkHttpClient\$Builder",
            "eventListenerFactory",
            "Lokhttp3/EventListener\$Factory;"
        )

        // Call SentryOkHttpEventListener constructor passing "eventListenerFactory" as parameter
        visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "io/sentry/android/okhttp/SentryOkHttpEventListener",
            "<init>",
            "(Lokhttp3/EventListener\$Factory;)V",
            false
        )

        // Call "eventListener" function of OkHttpClient.Builder passing SentryOkHttpEventListener
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "okhttp3/OkHttpClient\$Builder",
            "eventListener",
            "(Lokhttp3/EventListener;)Lokhttp3/OkHttpClient\$Builder;",
            false
        )
    }
}
