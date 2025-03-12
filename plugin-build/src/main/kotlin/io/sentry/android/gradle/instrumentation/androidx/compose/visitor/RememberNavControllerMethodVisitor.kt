package io.sentry.android.gradle.instrumentation.androidx.compose.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

class RememberNavControllerMethodVisitor(
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
    private val replacement = Replacement(
        "Lio/sentry/compose/SentryNavigationIntegrationKt;",
        "withSentryObservableEffect",
        "(Landroidx/navigation/NavHostController;Landroidx/compose/runtime/Composer;I)Landroidx/navigation/NavHostController;"
    )

    override fun onMethodExit(opcode: Int) {
        // NavHostController is the return value;
        // thus it's already on top of stack

        // Composer $composer
        loadArg(1)

        // int $changed
        loadArg(2)

        invokeStatic(
            Type.getType(replacement.owner),
            Method(replacement.name, replacement.descriptor)
        )
        super.onMethodExit(opcode)
    }
}
