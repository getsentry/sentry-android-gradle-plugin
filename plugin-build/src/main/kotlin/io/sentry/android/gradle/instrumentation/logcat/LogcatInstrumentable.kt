package io.sentry.android.gradle.instrumentation.logcat

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor

class SentryLogcatInstrumentable(private val minLevel: LogcatLevel) :
    ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        return LogcatClassVisitor(apiVersion, originalVisitor, minLevel)
    }

    override fun isInstrumentable(data: ClassContext) =
        !data.isSentryClass()
}
