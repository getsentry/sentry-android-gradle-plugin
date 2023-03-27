package io.sentry.android.gradle.instrumentation.logcat

import LogcatMethodInstrumentable
import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.*
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor

class LogcatInstrumentable :
    ClassInstrumentable {

    companion object {
        private const val LOG_CLASSNAME = "Log"
    }

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        val logcatMethodList: List<MethodInstrumentable> = listOf(
            LogcatMethodInstrumentable()
        )
        return CommonClassVisitor(
            apiVersion,
            originalVisitor,
            LOG_CLASSNAME,
            logcatMethodList,
            parameters
        )
    }

    override fun isInstrumentable(data: ClassContext) =
        !data.isSentryClass()
}
