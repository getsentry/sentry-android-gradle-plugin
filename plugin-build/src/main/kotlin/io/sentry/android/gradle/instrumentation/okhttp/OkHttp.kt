package io.sentry.android.gradle.instrumentation.okhttp

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.okhttp.visitor.ResponseWithInterceptorChainMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class OkHttp : ClassInstrumentable {
    override val fqName: String get() = "okhttp3.internal.connection.RealCall"

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = fqName.substringAfterLast('.'),
        methodInstrumentables = listOf(ResponseWithInterceptorChain()),
        parameters = parameters
    )
}

class ResponseWithInterceptorChain : MethodInstrumentable {
    override val fqName: String get() = "getResponseWithInterceptorChain"

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ResponseWithInterceptorChainMethodVisitor(
        api = apiVersion,
        originalVisitor = originalVisitor,
        access = instrumentableContext.access,
        name = instrumentableContext.name,
        descriptor = instrumentableContext.descriptor
    )

    override fun isInstrumentable(data: MethodContext): Boolean {
        return data.name?.startsWith(fqName) == true
    }
}
