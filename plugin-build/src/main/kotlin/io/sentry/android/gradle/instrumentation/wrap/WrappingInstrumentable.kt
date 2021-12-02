@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.wrap

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.wrap.visitor.WrappingVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class WrappingInstrumentable : ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        val className = instrumentableContext.currentClassData.className.substringAfterLast('.')
        return CommonClassVisitor(
            apiVersion = apiVersion,
            classVisitor = originalVisitor,
            className = className,
            methodInstrumentables = listOf(Wrap(className)),
            parameters = parameters
        )
    }

    override fun isInstrumentable(data: ClassContext): Boolean {
        return when {
            data.currentClassData.className.startsWith("io.sentry")
                && !data.currentClassData.className.startsWith("io.sentry.android.roomsample") -> false
            else -> true
        }
    }
}

class Wrap(private val className: String) : MethodInstrumentable {

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
            className = className,
            context = instrumentableContext,
            replacements = replacements
        )

    override fun isInstrumentable(data: MethodContext): Boolean = true
}
