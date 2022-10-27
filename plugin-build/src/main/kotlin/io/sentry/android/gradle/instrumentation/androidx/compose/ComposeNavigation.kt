package io.sentry.android.gradle.instrumentation.androidx.compose

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

open class ComposeNavigation : ClassInstrumentable {

    companion object {
        private const val NAV_HOST_CONTROLLER_CLASSNAME = "androidx.navigation.compose.NavHostControllerKt"
        private const val REMEMBER_NAV_CONTROLLER_NAME = "rememberNavController"

        /* ktlint-disable max-line-length */
        val replacement = Replacement(
            "Lio/sentry/compose/SentryNavigationIntegrationKt;",
            "withSentryObservableEffect",
            "(Landroidx/navigation/NavHostController;Landroidx/compose/runtime/Composer;I)Landroidx/navigation/NavHostController;"
        )
        /* ktlint-enable max-line-length */
    }

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        return object : ClassVisitor(apiVersion, originalVisitor) {

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor {
                val methodVisitor =
                    originalVisitor.visitMethod(access, name, descriptor, signature, exceptions)

                return if (name == REMEMBER_NAV_CONTROLLER_NAME) {
                    object : AdviceAdapter(apiVersion, methodVisitor, access, name, descriptor) {

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
                } else {
                    methodVisitor
                }
            }
        }
    }

    override fun isInstrumentable(data: ClassContext): Boolean {
        return data.currentClassData.className == NAV_HOST_CONTROLLER_CLASSNAME
    }
}
