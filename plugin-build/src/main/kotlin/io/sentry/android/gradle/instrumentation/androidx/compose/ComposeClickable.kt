package io.sentry.android.gradle.instrumentation.androidx.compose

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.compose.visitor.ModifierClickableMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

open class ComposeClickable : ClassInstrumentable {

    companion object {
        private const val CLICKABLE_CLASSNAME =
            "androidx.compose.foundation.ClickableKt"
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
            CLICKABLE_CLASSNAME,
            listOf(object : MethodInstrumentable {

                override val fqName: String get() = ""

                /* ktlint-disable max-line-length */
                override fun isInstrumentable(data: MethodContext): Boolean {
                    return data.name?.startsWith("clickable") ?: false &&
                        data.descriptor == "(Landroidx/compose/ui/Modifier;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)Landroidx/compose/ui/Modifier;"
                }
                /* ktlint-enable max-line-length */

                override fun getVisitor(
                    instrumentableContext: MethodContext,
                    apiVersion: Int,
                    originalVisitor: MethodVisitor,
                    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
                ): MethodVisitor {
                    return ModifierClickableMethodVisitor(
                        instrumentableContext,
                        apiVersion,
                        originalVisitor
                    )
                }
            }),
            parameters
        )
    }

    override fun isInstrumentable(data: ClassContext): Boolean {
        return data.currentClassData.className == CLICKABLE_CLASSNAME
    }
}
