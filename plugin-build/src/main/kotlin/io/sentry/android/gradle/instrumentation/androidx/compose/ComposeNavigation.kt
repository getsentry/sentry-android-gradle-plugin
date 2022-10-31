package io.sentry.android.gradle.instrumentation.androidx.compose

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

open class ComposeNavigation : ClassInstrumentable {

    companion object {
        private const val NAV_HOST_CONTROLLER_CLASSNAME =
            "androidx.navigation.compose.NavHostControllerKt"

        /* ktlint-disable max-line-length */
        private val replacement = Replacement(
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
        return CommonClassVisitor(
            apiVersion,
            originalVisitor,
            NAV_HOST_CONTROLLER_CLASSNAME,
            listOf(object : MethodInstrumentable {

                override val fqName: String get() = "rememberNavController"

                override fun getVisitor(
                    instrumentableContext: MethodContext,
                    apiVersion: Int,
                    originalVisitor: MethodVisitor,
                    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
                ): MethodVisitor {
                    return object : AdviceAdapter(
                        apiVersion,
                        originalVisitor,
                        instrumentableContext.access,
                        instrumentableContext.name,
                        instrumentableContext.descriptor
                    ) {
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
                }
            }),
            parameters
        )
    }

    override fun isInstrumentable(data: ClassContext): Boolean {
        return data.currentClassData.className == NAV_HOST_CONTROLLER_CLASSNAME
    }
}
