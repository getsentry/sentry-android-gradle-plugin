package io.sentry.android.gradle.instrumentation.appstart

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ContentProvider : ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = "ContentProvider",
        methodInstrumentables = listOf(ContentProviderMethodInstrumentable()),
        parameters = parameters
    )

    override fun isInstrumentable(data: ClassContext) =
        data.currentClassData.superClasses.contains("android.content.ContentProvider")
}

class ContentProviderMethodInstrumentable : MethodInstrumentable {

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ContentProviderMethodVisitor(
        apiVersion = apiVersion,
        originalVisitor = originalVisitor,
        instrumentableContext = instrumentableContext,
    )

    override fun isInstrumentable(data: MethodContext): Boolean {
        // TODO: think about constructors as well
        // <init>, ()V
        // <init>, (I)V
        // <clinit>, ()V

        // public boolean onCreate()
        // onCreate, ()Z
        return data.name == "onCreate" && data.descriptor == "()Z"
    }
}
