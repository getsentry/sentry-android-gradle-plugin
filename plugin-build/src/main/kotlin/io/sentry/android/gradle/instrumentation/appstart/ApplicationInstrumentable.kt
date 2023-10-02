package io.sentry.android.gradle.instrumentation.appstart

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ApplicationInstrumentable : ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = "Application",
        methodInstrumentables = listOf(ApplicationMethodInstrumentable()),
        parameters = parameters
    )

    override fun isInstrumentable(data: ClassContext): Boolean =
        data.currentClassData.superClasses.contains("android.app.Application")
}

class ApplicationMethodInstrumentable : MethodInstrumentable {

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ApplicationMethodVisitor(
        apiVersion = apiVersion,
        originalVisitor = originalVisitor,
        instrumentableContext = instrumentableContext,
    )

    override fun isInstrumentable(data: MethodContext): Boolean {
        // TODO: think about constructors as well
        // <init>, ()V
        // <clinit>, ()V

        // public void onCreate()
        // onCreate, ()V
        return data.name == "onCreate" && data.descriptor == "()V"
    }
}
