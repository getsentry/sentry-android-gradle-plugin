@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.wrap

import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.util.AnalyzingVisitor
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import io.sentry.android.gradle.instrumentation.wrap.visitor.WrappingVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode

class WrappingInstrumentable : ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        val simpleClassName =
            instrumentableContext.currentClassData.className.substringAfterLast('.')
        return AnalyzingVisitor(
            apiVersion = apiVersion,
            nextVisitor = { methods ->
                CommonClassVisitor(
                    apiVersion = apiVersion,
                    classVisitor = originalVisitor,
                    className = simpleClassName,
                    methodInstrumentables = methods.map {
                        Wrap(instrumentableContext.currentClassData, it)
                    },
                    parameters = parameters
                )
            }
        )
    }

    override fun isInstrumentable(data: ClassContext): Boolean = !data.isSentryClass()
}

class Wrap(
    private val classContext: ClassData,
    private val methodNode: MethodNode
) : MethodInstrumentable {

    override val fqName: String = methodNode.name

    companion object {
        private val replacements = mapOf(
            // FileInputStream to SentryFileInputStream
            Replacement.FileInputStream.STRING,
            Replacement.FileInputStream.FILE,
            Replacement.FileInputStream.FILE_DESCRIPTOR,
            // FileOutputStream to SentryFileOutputStream
            Replacement.FileOutputStream.STRING,
            Replacement.FileOutputStream.STRING_BOOLEAN,
            Replacement.FileOutputStream.FILE,
            Replacement.FileOutputStream.FILE_BOOLEAN,
            Replacement.FileOutputStream.FILE_DESCRIPTOR
            // TODO: enable, once https://github.com/getsentry/sentry-java/issues/1842 is resolved
            // Context.openFileInput to SentryFileInputStream
//            Replacement.Context.OPEN_FILE_INPUT,
            // Context.openFileOutput to SentryFileOutputStream
//            Replacement.Context.OPEN_FILE_OUTPUT
        )
    }

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor =
        WrappingVisitor(
            api = apiVersion,
            originalVisitor = originalVisitor,
            firstPassVisitor = methodNode,
            classContext = classContext,
            context = instrumentableContext,
            replacements = replacements
        )

    override fun isInstrumentable(data: MethodContext): Boolean =
        data.name == fqName &&
            data.descriptor == methodNode.desc &&
            data.access == methodNode.access
}
