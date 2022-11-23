package io.sentry.android.gradle.instrumentation.androidx.compose.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

class ModifierClickableMethodVisitor(
    methodContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor
) : AdviceAdapter(
    apiVersion,
    originalVisitor,
    methodContext.access,
    methodContext.name,
    methodContext.descriptor
) {
    override fun onMethodEnter() {
        super.onMethodEnter()
        val clickableIdx = argumentTypes.indexOfFirst {
            it.descriptor == "Lkotlin/jvm/functions/Function0;"
        }
        val clickLabelIdx = argumentTypes.indexOfFirst {
            it.descriptor == "Ljava/lang/String;"
        }
        /* ktlint-disable max-line-length */
        if (clickableIdx != -1 && clickLabelIdx != -1) {
            loadArg(clickableIdx)
            loadArg(clickLabelIdx)

            invokeStatic(
                Type.getType("Lio/sentry/compose/SentryClickableIntegrationKt;"),
                Method(
                    "wrapClickable",
                    "(Lkotlin/jvm/functions/Function0;Ljava/lang/String;)Lkotlin/jvm/functions/Function0;"
                )
            )
            storeArg(clickableIdx)
        }
        /* ktlint-enable max-line-length */
    }
}
