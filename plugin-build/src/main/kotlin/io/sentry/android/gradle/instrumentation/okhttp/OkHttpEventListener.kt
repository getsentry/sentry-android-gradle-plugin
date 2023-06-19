package io.sentry.android.gradle.instrumentation.okhttp

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.okhttp.visitor.OkHttpEventListenerMethodVisitor
import io.sentry.android.gradle.util.SemVer
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class OkHttpEventListener(private val okHttpVersion: SemVer) : ClassInstrumentable {
    override val fqName: String get() = "okhttp3.OkHttpClient"

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = fqName.substringAfterLast('.'),
        methodInstrumentables = listOf(OkHttpEventListenerMethodInstrumentable(okHttpVersion)),
        parameters = parameters
    )
}

class OkHttpEventListenerMethodInstrumentable(
    private val okHttpVersion: SemVer
) : MethodInstrumentable {
    override val fqName: String get() = "<init>"

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = OkHttpEventListenerMethodVisitor(
        apiVersion = apiVersion,
        originalVisitor = originalVisitor,
        instrumentableContext = instrumentableContext,
        okHttpVersion = okHttpVersion
    )

    override fun isInstrumentable(data: MethodContext): Boolean {
        return data.name == fqName && data.descriptor == "(Lokhttp3/OkHttpClient\$Builder;)V"
    }
}
